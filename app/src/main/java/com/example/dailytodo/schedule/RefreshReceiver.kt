package com.example.dailytodo.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailytodo.domain.TaskSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 闹钟到点后的回调：补算 → 落库 → 重绘小部件 → 排下一个闹钟。
 * 全部逻辑都在 TaskSync 里，这里只负责把协程生命周期接住。
 */
class RefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        // goAsync() 让广播在协程跑完之前不被系统回收（最多约 10 秒）
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
