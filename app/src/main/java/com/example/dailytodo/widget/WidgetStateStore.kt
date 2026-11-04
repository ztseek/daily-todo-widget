package com.example.dailytodo.widget

import android.content.Context

/** 撤销条快照 */
data class UndoSnapshot(val taskId: Long, val title: String, val at: Long)

/**
 * 小部件与进程之间的临时状态。
 *
 * 为什么不在数据库里存「刚刚点了哪条」：
 * 这是个纯展示状态，5 秒后就该消失，放进数据库会让 Room 的 Flow
 * 频繁发射、列表闪一下。放 SharedPreferences 更合适。
 */
object WidgetStateStore {

    private const val PREFS = "widget_state"
    private const val KEY_TASK_ID = "undo_task_id"
    private const val KEY_TITLE = "undo_title"
    private const val KEY_AT = "undo_at"

    /** 撤销窗口时长：5 秒 */
    const val UNDO_WINDOW_MS = 5_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveUndo(context: Context, taskId: Long, title: String) {
        prefs(context).edit()
            .putLong(KEY_TASK_ID, taskId)
            .putString(KEY_TITLE, title)
            .putLong(KEY_AT, System.currentTimeMillis())
            .apply()
    }

    /** 读取未过期的撤销快照；过期返回 null */
    fun readUndo(context: Context): UndoSnapshot? {
        val p = prefs(context)
        val id = p.getLong(KEY_TASK_ID, -1L)
        val at = p.getLong(KEY_AT, 0L)
        if (id <= 0L || at <= 0L) return null
        if (System.currentTimeMillis() - at > UNDO_WINDOW_MS) return null
        return UndoSnapshot(id, p.getString(KEY_TITLE, "") ?: "", at)
    }

    fun clearUndo(context: Context) {
        prefs(context).edit()
            .remove(KEY_TASK_ID)
            .remove(KEY_TITLE)
            .remove(KEY_AT)
            .apply()
    }
}
