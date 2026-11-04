package com.example.dailytodo.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * 闹钟调度器。
 *
 * ## 为什么不用 updatePeriodMillis 就够
 *
 * 小部件的 updatePeriodMillis 系统最小只接受 30 分钟，而且**不保证准时**
 * （Doze、省电模式下会被推迟甚至跳过）。要做到「每天 08:00 准时回来」，
 * 必须自己用 AlarmManager 排一个精确闹钟。
 *
 * ## 为什么只排一个闹钟
 *
 * 不为每条任务都排闹钟，只排「所有任务里最早的那个 nextDueAt」。
 * 触发后重新补算、重绘、再排下一个 —— 这叫「最近到期」策略，
 * 无论有 1 条还是 100 条任务，系统里永远只有 1 个待触发的闹钟，省电且不会乱。
 */
object AlarmScheduler {

    private const val REQUEST_CODE_REFRESH = 1001

    /**
     * 排下一次刷新。传 null 表示当前没有需要刷新的任务，把旧的取消掉。
     */
    fun schedule(context: Context, atMillis: Long?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = refreshPendingIntent(context)

        // 先无条件取消，避免旧闹钟和新闹钟同时存在导致重复触发
        alarmManager.cancel(pendingIntent)

        if (atMillis == null || atMillis <= 0L) return

        // 多给 1 秒，避开"刚好等于边界"时 catchUp 推进不动的浮点/精度问题
        val triggerAt = maxOf(atMillis + 1_000L, System.currentTimeMillis() + 1_000L)

        try {
            if (canScheduleExact(alarmManager)) {
                // 精确闹钟，Doze 模式下也能唤醒
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            } else {
                // 没拿到精确闹钟权限时的降级：允许 15 分钟误差窗口。
                // 不降级的话这里会直接抛 SecurityException 让 app 崩掉。
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP, triggerAt, 15 * 60 * 1000L, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "精确闹钟被拒绝，降级为普通闹钟", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12 以下默认就有
        }

    /** 供界面跳系统设置用 */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }

    private fun refreshPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REFRESH,
            Intent(context, RefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
