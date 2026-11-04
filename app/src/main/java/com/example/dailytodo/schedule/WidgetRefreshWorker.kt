package com.example.dailytodo.schedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailytodo.domain.TaskSync

/**
 * 兜底刷新。
 *
 * 国产 ROM 经常把 AlarmManager 的闹钟清掉，或者用户手动"深度清理"过后台。
 * 这时闹钟链就断了。WorkManager 的周期性任务是系统级调度，存活率高得多，
 * 每 6 小时把数据补算一次，最多让用户看到「晚了 6 小时」而不是「再也不刷新」。
 *
 * 注意它只做兜底，准时性靠 AlarmManager —— 两者是主力和保险的关系，不能互相替代。
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        TaskSync.syncAndRefresh(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "widget-fallback-refresh"
    }
}
