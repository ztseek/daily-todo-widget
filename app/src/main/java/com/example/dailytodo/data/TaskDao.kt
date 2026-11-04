package com.example.dailytodo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** 界面订阅用 */
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Task>>

    /** 后台逻辑用（闹钟、小部件渲染） */
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 下一次需要重绘小部件的时间 = 所有周期任务里最小的 nextDueAt。
     * 一次性任务没有"下次"，所以排除掉。
     */
    @Query("SELECT MIN(nextDueAt) FROM tasks WHERE repeatType <> 'ONCE'")
    suspend fun earliestNextDueAt(): Long?
}
