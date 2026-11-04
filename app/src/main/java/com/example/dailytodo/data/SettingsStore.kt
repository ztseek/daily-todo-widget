package com.example.dailytodo.data

import android.content.Context

/**
 * 用户设置。只有一项是必须的，其余是引导用的开关状态。
 * 用 SharedPreferences 而不是 DataStore，是为了让小部件的 onUpdate 里读取零成本。
 */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /**
     * 逾期是否累计。
     * 默认 false —— 到期没点掉的任务原样留在小部件上，不记逾期次数。
     * 打开后：每越过一个刷新锚点仍未完成，overdueCount + 1，小部件上标红显示。
     */
    var countOverdue: Boolean
        get() = prefs.getBoolean(KEY_COUNT_OVERDUE, false)
        set(value) = prefs.edit().putBoolean(KEY_COUNT_OVERDUE, value).apply()

    companion object {
        private const val KEY_COUNT_OVERDUE = "count_overdue"

        fun get(context: Context): SettingsStore = SettingsStore(context)
    }
}
