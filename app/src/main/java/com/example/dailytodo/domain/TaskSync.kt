package com.example.dailytodo.domain

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.dailytodo.data.SettingsStore
import com.example.dailytodo.data.Task
import com.example.dailytodo.data.TaskRepository
import com.example.dailytodo.schedule.AlarmScheduler
import com.example.dailytodo.widget.TaskWidget
import java.time.ZoneId

/**
 * 同步编排层。所有入口（界面操作、闹钟、开机、兜底 Worker、小部件渲染）
 * 都只调这里，保证「补算 → 落库 → 重绘 → 重排闹钟」四步永远是一起发生的。
 *
 * 少任何一步都会出问题：
 *  - 漏了补算：过了 8 点任务不回来
 *  - 漏了落库：补齐结果每次都要重算，逾期次数会算重
 *  - 漏了重绘：数据库对了但桌面还是旧的
 *  - 漏了重排闹钟：下一次就再也不会自动刷新了
 */
object TaskSync {

    /**
     * 补算所有任务并把结果落库，返回**当前应显示在小部件上的任务**。
     *
     * 关键点：这个函数是幂等的，所以可以放心地在每次小部件渲染前调用。
     * 这就意味着——哪怕闹钟被国产 ROM 杀掉了，用户只要回一次桌面
     * 或者点一下任意待办，显示也是准的。闹钟只是"锦上添花的准时"。
     */
    suspend fun loadVisible(context: Context): List<Task> {
        val repo = TaskRepository.get(context)
        val countOverdue = SettingsStore.get(context).countOverdue
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val all = repo.getAll()
        val result = ArrayList<Task>(all.size)
        for (task in all) {
            val fixed = RecurrenceEngine.catchUp(task, now, countOverdue, zone)
            if (fixed != task) repo.update(fixed)
            result += fixed
        }
        return result.filter { it.completedAt == null }
    }

    /** 数据发生变化后的完整刷新链路 */
    suspend fun syncAndRefresh(context: Context) {
        loadVisible(context)
        TaskWidget().updateAll(context)
        rescheduleAlarm(context)
    }

    /** 只重排闹钟（小部件自己重绘时用，避免打转） */
    suspend fun rescheduleAlarm(context: Context) {
        val earliest = TaskRepository.get(context).earliestNextDueAt()
        AlarmScheduler.schedule(context, earliest)
    }
}
