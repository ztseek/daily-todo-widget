package com.example.dailytodo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.dailytodo.data.TaskRepository
import com.example.dailytodo.domain.TaskSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 把 taskId 从点击事件传进回调用的小部件专用参数键 */
val TaskIdKey = ActionParameters.Key<Long>("taskId")

/**
 * 点击一条待办 = 标记本轮完成。
 *
 * 注意：小部件的点击回调运行在 BroadcastReceiver 里，生命周期很短，
 * 所以不能在这里做耗时的 UI 操作，只做「写库 + 重绘 + 排撤销过期」。
 */
class CompleteAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val repo = TaskRepository.get(context)
        val task = repo.getById(taskId) ?: return

        repo.complete(taskId)
        WidgetStateStore.saveUndo(context, taskId, task.title)

        TaskWidget().updateAll(context)
        TaskSync.rescheduleAlarm(context)
        scheduleUndoExpiry(context)
    }

    /** 5 秒后把撤销条撤掉并重绘一次（否则那行字会一直挂在小部件上） */
    private fun scheduleUndoExpiry(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + WidgetStateStore.UNDO_WINDOW_MS,
            PendingIntent.getBroadcast(
                context,
                2002,
                Intent(context, UndoExpiryReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}

/** 点撤销 = 把 completedAt 清空，任务立刻回到小部件 */
class UndoAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val snapshot = WidgetStateStore.readUndo(context) ?: return
        TaskRepository.get(context).undoComplete(snapshot.taskId)
        WidgetStateStore.clearUndo(context)
        TaskWidget().updateAll(context)
        TaskSync.rescheduleAlarm(context)
    }
}

/** 撤销窗口到期，清掉撤销条 */
class UndoExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetStateStore.clearUndo(appContext)
                TaskWidget().updateAll(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
