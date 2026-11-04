package com.example.dailytodo.domain

import com.example.dailytodo.data.RepeatType
import com.example.dailytodo.data.Task
import com.example.dailytodo.data.WeekdayMask

/** 把任务规则翻译成人话，界面和小部件共用一套文案，避免两处不一致 */
object RecurrenceText {

    /** 例：每天 08:00 / 每周一、三 10:00 / 每月 1 日 09:00 / 每 3 天 07:30 / 一次性 */
    fun describe(task: Task): String {
        val time = "%02d:%02d".format(task.refreshHour, task.refreshMinute)
        return when (task.repeatType) {
            RepeatType.ONCE -> "一次性"
            RepeatType.DAILY -> "每天 $time"
            RepeatType.WEEKLY -> "每周${WeekdayMask.toChinese(task.weekdays)} $time"
            RepeatType.MONTHLY -> "每月 ${task.monthDay} 日 $time"
            RepeatType.EVERY_N_DAYS -> "每 ${task.intervalDays.coerceAtLeast(1)} 天 $time"
        }
    }

    /** 小部件上的短标签，尽量控制在 6 个字以内 */
    fun shortLabel(task: Task): String = when (task.repeatType) {
        RepeatType.ONCE -> "一次性"
        RepeatType.DAILY -> "每日"
        RepeatType.WEEKLY -> "每周${WeekdayMask.toChinese(task.weekdays)}"
        RepeatType.MONTHLY -> "每月${task.monthDay}日"
        RepeatType.EVERY_N_DAYS -> "每${task.intervalDays.coerceAtLeast(1)}天"
    }

    /** 「下次出现」的友好描述，列表页用 */
    fun nextTimeHint(task: Task): String {
        if (task.repeatType == RepeatType.ONCE) {
            return if (task.completedAt == null) "完成后不再出现" else "已完成"
        }
        if (task.completedAt == null) return "本轮待完成"
        val text = describe(task)
        return "下次：$text"
    }
}
