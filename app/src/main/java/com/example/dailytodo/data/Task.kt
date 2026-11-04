package com.example.dailytodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * 刷新周期类型。
 *
 * WEEKLY 同时覆盖「每周刷新」和「星期几刷新」两种需求：
 * weekdays 用位掩码表示，可以同时选周一、周三、周五。
 */
enum class RepeatType {
    /** 一次性任务：点掉就永久消失，不会回来 */
    ONCE,

    /** 每天，在 refreshHour:refreshMinute 这个时刻刷新 */
    DAILY,

    /** 每周，在 weekdays 指定的星期几刷新（可多选） */
    WEEKLY,

    /** 每月，在 monthDay 指定的日期刷新（31 号在小月自动落到月末） */
    MONTHLY,

    /** 每隔 intervalDays 天刷新一次 */
    EVERY_N_DAYS,
}

/**
 * 一条待办。
 *
 * 设计上最重要的一点：
 * 任务的「当前状态」不是靠一个布尔值 completed 表示的，而是靠三元组
 *   (nextDueAt, completedAt, overdueCount)
 * 共同描述。原因见 DESIGN.md「状态模型」一节。
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 待办内容 */
    val title: String,

    /** 备注，小部件上不显示 */
    val note: String = "",

    /** 手排顺序，越小越靠前 */
    val sortOrder: Int = 0,

    // ---------- 周期规则 ----------
    val repeatType: RepeatType = RepeatType.DAILY,

    /** EVERY_N_DAYS 用：间隔天数，最小 1 */
    val intervalDays: Int = 2,

    /** WEEKLY 用：位掩码，bit0=周一 … bit6=周日。0 表示"跟创建日同一星期几" */
    val weekdays: Int = 0,

    /** MONTHLY 用：1~31，超出当月天数取月末 */
    val monthDay: Int = 1,

    /** 锚定刷新时刻：周期推进时统一用这个时:分 */
    val refreshHour: Int = 8,
    val refreshMinute: Int = 0,

    // ---------- 状态 ----------
    /**
     * 本轮锚点时间戳 —— 语义是「最近一次已经到点的刷新时刻」。
     * 它永远 <= now（除非是一次性任务）。
     * 注意它不是"下一次刷新时间"，这个区别是整套算法的关键。
     */
    val nextDueAt: Long = 0L,

    /** 本轮完成时间。非 null = 本轮已完成 = 从小部件上隐藏 */
    val completedAt: Long? = null,

    /** 逾期次数。仅在设置里打开「逾期计数」后才累加 */
    val overdueCount: Int = 0,
)

/** Room 需要一个显式的枚举转换器（不同 Room 版本对 enum 的默认支持不一致，写死最稳） */
class Converters {
    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType =
        runCatching { RepeatType.valueOf(value) }.getOrDefault(RepeatType.DAILY)
}

/** 星期掩码相关的工具 */
object WeekdayMask {
    /** 周一是 bit0，周二是 bit1 …… 周日是 bit6（跟 java.time.DayOfWeek.value 1..7 对齐） */
    fun bitOf(dayOfWeekValue: Int): Int = 1 shl (dayOfWeekValue - 1)

    fun contains(mask: Int, dayOfWeekValue: Int): Boolean = mask and bitOf(dayOfWeekValue) != 0

    /** 把掩码转成「一、三、五」这样的中文，供界面和小部件展示 */
    fun toChinese(mask: Int): String {
        val names = arrayOf("一", "二", "三", "四", "五", "六", "日")
        val picked = names.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
        return if (picked.isEmpty()) "一" else picked.joinToString("、")
    }
}
