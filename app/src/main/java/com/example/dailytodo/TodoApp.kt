package com.example.dailytodo

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dailytodo.domain.TaskSync
import com.example.dailytodo.schedule.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 应用入口。只做两件事：
 * 1. 注册 6 小时一次的兜底刷新（KEEP 策略保证不会重复注册）
 * 2. 冷启动时立刻补算一次 —— 用户打开 app 往往就是"过了一天"的时候
 */
class TodoApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val fallback = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            fallback
        )

        appScope.launch { TaskSync.syncAndRefresh(this@TodoApp) }
    }
}
