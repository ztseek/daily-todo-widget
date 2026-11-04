package com.example.dailytodo.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dailytodo.domain.TaskSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机 / 应用升级后的重建。
 *
 * 必须处理，否则：手机关机一晚 → 早上开机 → AlarmManager 里的闹钟全被清空
 * → 今天的任务再也不会自动回来（除非用户手动打开 app）。
 * 这是这类 app 最典型的"用两天就坏"的 bug 来源。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        Log.i("BootReceiver", "收到 $action，重建刷新链")
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TaskSync.syncAndRefresh(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
