package com.example.dailytodo.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 数据访问门面。
 *
 * 这里刻意不引入 Hilt / Koin：这个 app 只有一张表、一个仓库，
 * 手写一个 `get(context)` 单例足够，还能少 20 行构建配置和一堆注解。
 */
class TaskRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    suspend fun getAll(): List<Task> = dao.getAll()

    suspend fun getById(id: Long): Task? = dao.getById(id)

    suspend fun earliestNextDueAt(): Long? = dao.earliestNextDueAt()

    /** 新建时 nextDueAt 直接取当前时间：用户刚写下的待办必须立刻出现在小部件上 */
    suspend fun insert(task: Task): Long = dao.insert(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** 点一下 = 标记本轮完成 = 立刻从小部件隐藏 */
    suspend fun complete(id: Long, at: Long = System.currentTimeMillis()) {
        dao.getById(id)?.let { dao.update(it.copy(completedAt = at)) }
    }

    /** 撤销 = 把 completedAt 清空 */
    suspend fun undoComplete(id: Long) {
        dao.getById(id)?.let { dao.update(it.copy(completedAt = null)) }
    }

    companion object {
        fun get(context: Context): TaskRepository =
            TaskRepository(AppDatabase.get(context).taskDao())
    }
}
