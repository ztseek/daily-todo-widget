package com.example.dailytodo.domain

import com.example.dailytodo.data.RepeatType
import com.example.dailytodo.data.Task
import com.example.dailytodo.data.WeekdayMask
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.min

/**
 * 周期引擎 —— 这个 app 唯一真正需要用脑子写的部分。
 *
 * ## 核心语义：锚定刷新时刻
 *
 * 「每日 08:00 刷新」的意思是：
 *   - 今天 14:00 点掉 → 明天 08:00 回来（不是明天 14:00）
 *   - 明天还是 08:00 回来（时刻永远不漂移）
 *
 * 实现方式：每条任务存一个 `nextDueAt`，语义是**最近一次已到点的锚点**。
 * 推进规则是「日期按周期走，时:分永远用 refreshHour:refreshMinute」，
 * 所以时刻天然不会漂移。
 *
 * ## 为什么不用「下一次到期时间」做字段
 *
 * 如果字段是"下一次时间"（未来时间），那么渲染前必须先判断 now >= 该时间
 * 才能决定显示；而「刚创建的任务要立刻显示」又要求它是过去时间 —— 两者矛盾，
 * 会写出一堆特例。用「最近已到点」做语义，规则就统一了：
 *   - 创建时 nextDueAt = now（立刻可见）
 *   - 每越过一个锚点就推进一格（并视设置决定是否记逾期）
 *   - 已完成的任务靠 completedAt 隐藏，而不是靠时间比较
 */
object RecurrenceEngine {

    /**
     * 从 base 这个锚点往后推一格，返回下一格的锚点时间戳（毫秒）。
     *
     * 注意「日期按周期走 + 时分用 refreshTime」这个组合：这是时刻不漂移的关键。
     */
    fun nextAnchorAfter(baseMillis: Long, task: Task, zone: ZoneId = ZoneId.systemDefault()): Long {
        val baseDate = Instant.ofEpochMilli(baseMillis).atZone(zone).toLocalDate()
        val atTime = LocalTime.of(
            task.refreshHour.coerceIn(0, 23),
            task.refreshMinute.coerceIn(0, 59)
        )

        val nextDate: LocalDate = when (task.repeatType) {
            // 一次性任务没有下一格。这里给一个很远的日期，让调用方永远不会被触发。
            RepeatType.ONCE -> baseDate.plusYears(100)
            RepeatType.DAILY -> baseDate.plusDays(1)
            RepeatType.EVERY_N_DAYS -> baseDate.plusDays(task.intervalDays.coerceAtLeast(1).toLong())
            RepeatType.WEEKLY -> nextWeekday(baseDate, task.weekdays)
            RepeatType.MONTHLY -> nextMonthDay(baseDate, task.monthDay)
        }

        return nextDate.atTime(atTime).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * 把一条任务从「上次同步到的时间」补齐到 now。
     *
     * 这个函数必须满足两个性质：
     * 1. **幂等** —— 同一时刻反复调用，结果一致（因为 nextDueAt 会被推进到未来）。
     * 2. **能跨越多个周期** —— 手机关机一周再打开，不能只推进一格，
     *    否则要连点七次才回到今天。所以用 while 循环一直推到追平现在为止。
     *
     * @param countOverdue 设置里的「逾期计数」开关；关闭时只是重置、不计数
     */
    fun catchUp(
        task: Task,
        nowMillis: Long,
        countOverdue: Boolean,
        zone: ZoneId = ZoneId.systemDefault()
    ): Task {
        if (task.repeatType == RepeatType.ONCE) return task

        var current = task
        // 防御性上限：即使数据有问题（比如 nextDueAt 被写成 0）也不会死循环
        var guard = 0
        while (guard++ < 1000) {
            val next = nextAnchorAfter(current.nextDueAt, current, zone)
            if (next > nowMillis) break

            // 越过了一个锚点：
            // 如果这一轮还没被完成 → 视设置累计一次逾期
            if (current.completedAt == null && countOverdue) {
                current = current.copy(overdueCount = current.overdueCount + 1)
            }
            // 无论如何都进入新一轮：清空完成标记（让它重新出现），锚点前进一格
            current = current.copy(nextDueAt = next, completedAt = null)
        }
        return current
    }

    /** 严格在 from 之后（不含 from 当天）的、符合 WEEKLY 掩码的下一天 */
    private fun nextWeekday(from: LocalDate, mask: Int): LocalDate {
        // mask 为 0 时退化为「跟创建日同一星期几」
        val effective = if (mask == 0) WeekdayMask.bitOf(from.dayOfWeek.value) else mask
        var date = from.plusDays(1)
        var steps = 0
        while (steps < 7) {
            if (WeekdayMask.contains(effective, date.dayOfWeek.value)) return date
            date = date.plusDays(1)
            steps++
        }
        return date
    }

    /** 严格在 from 之后的、符合 MONTHLY 日期的下一天 */
    private fun nextMonthDay(from: LocalDate, monthDay: Int): LocalDate {
        val target = monthDay.coerceIn(1, 31)
        var yearMonth: YearMonth = YearMonth.from(from)
        // 31 号落在 2 月时取 28/29，落在小月时取 30 —— 这是真实日历里人的直觉
        var candidate = yearMonth.atDay(min(target, yearMonth.lengthOfMonth()))
        if (!candidate.isAfter(from)) {
            yearMonth = yearMonth.plusMonths(1)
            candidate = yearMonth.atDay(min(target, yearMonth.lengthOfMonth()))
        }
        return candidate
    }
}
