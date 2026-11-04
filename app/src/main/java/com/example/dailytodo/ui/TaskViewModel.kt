package com.example.dailytodo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytodo.data.RepeatType
import com.example.dailytodo.data.SettingsStore
import com.example.dailytodo.data.Task
import com.example.dailytodo.data.TaskRepository
import com.example.dailytodo.domain.TaskSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 编辑中的草稿。和 Task 分开，是为了让"取消"真的不会写库 */
data class TaskDraft(
    val id: Long = 0L,
    val title: String = "",
    val note: String = "",
    val repeatType: RepeatType = RepeatType.DAILY,
    val intervalDays: Int = 3,
    val weekdays: Int = 1,   // 位掩码，bit0 = 周一，所以 1 就是"只在周一"
    val monthDay: Int = 1,
    val refreshHour: Int = 8,
    val refreshMinute: Int = 0,
)

fun Task.toDraft() = TaskDraft(
    id = id,
    title = title,
    note = note,
    repeatType = repeatType,
    intervalDays = intervalDays,
    weekdays = if (weekdays == 0) 1 else weekdays,
    monthDay = monthDay,
    refreshHour = refreshHour,
    refreshMinute = refreshMinute,
)

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TaskRepository.get(app)
    private val settings = SettingsStore.get(app)

    val tasks: StateFlow<List<Task>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _countOverdue = MutableStateFlow(settings.countOverdue)
    val countOverdue: StateFlow<Boolean> = _countOverdue.asStateFlow()

    fun setCountOverdue(enabled: Boolean) {
        settings.countOverdue = enabled
        _countOverdue.value = enabled
        refresh()
    }

    /** 新建或更新。existing 为空表示新建 */
    fun saveDraft(draft: TaskDraft, existing: Task?) {
        val title = draft.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            if (existing == null) {
                repo.insert(
                    Task(
                        title = title,
                        note = draft.note,
                        sortOrder = (System.currentTimeMillis() / 1000L).toInt(),
                        repeatType = draft.repeatType,
                        intervalDays = draft.intervalDays.coerceAtLeast(1),
                        weekdays = draft.weekdays,
                        monthDay = draft.monthDay.coerceIn(1, 31),
                        refreshHour = draft.refreshHour,
                        refreshMinute = draft.refreshMinute,
                        // 新建立刻可见：锚点直接取当前时间
                        nextDueAt = System.currentTimeMillis()
                    )
                )
            } else {
                // 只改规则字段，nextDueAt / completedAt / overdueCount 保持原样，
                // 否则用户改个标题就会把"已完成"状态弄丢
                repo.update(
                    existing.copy(
                        title = title,
                        note = draft.note,
                        repeatType = draft.repeatType,
                        intervalDays = draft.intervalDays.coerceAtLeast(1),
                        weekdays = draft.weekdays,
                        monthDay = draft.monthDay.coerceIn(1, 31),
                        refreshHour = draft.refreshHour,
                        refreshMinute = draft.refreshMinute,
                    )
                )
            }
            TaskSync.syncAndRefresh(getApplication())
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            if (task.completedAt == null) repo.complete(task.id) else repo.undoComplete(task.id)
            TaskSync.syncAndRefresh(getApplication())
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repo.delete(task.id)
            TaskSync.syncAndRefresh(getApplication())
        }
    }

    /** 手动触发一次补算（下拉刷新或切换设置后） */
    fun refresh() {
        viewModelScope.launch { TaskSync.syncAndRefresh(getApplication()) }
    }
}
