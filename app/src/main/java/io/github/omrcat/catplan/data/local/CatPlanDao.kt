package io.github.omrcat.catplan.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {
    @Query("SELECT * FROM cats ORDER BY createdAtEpochMs")
    fun observeAll(): Flow<List<CatEntity>>

    @Query("SELECT * FROM cats ORDER BY createdAtEpochMs")
    suspend fun getAll(): List<CatEntity>

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getById(id: Long): CatEntity?

    @Insert
    suspend fun insert(cat: CatEntity): Long

    @Update
    suspend fun update(cat: CatEntity)

    @Delete
    suspend fun delete(cat: CatEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries WHERE catId = :catId ORDER BY dateEpochDay")
    fun observeForCat(catId: Long): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay")
    suspend fun getAll(): List<WeightEntryEntity>

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Delete
    suspend fun delete(entry: WeightEntryEntity)
}

@Dao
interface DailyDao {
    @Query("SELECT * FROM dailies WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC, createdAtEpochMs DESC")
    fun observeSince(sinceEpochDay: Long): Flow<List<DailyEntity>>

    @Query("SELECT * FROM dailies WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC, createdAtEpochMs DESC")
    suspend fun getSince(sinceEpochDay: Long): List<DailyEntity>

    @Query("SELECT * FROM dailies ORDER BY dateEpochDay")
    suspend fun getAll(): List<DailyEntity>

    /** 当天某类型的打卡记录；全家与按猫一并返回，由调用方按展示需要过滤 */
    @Query("SELECT * FROM dailies WHERE type = :type AND dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMs")
    suspend fun getForDay(type: String, dateEpochDay: Long): List<DailyEntity>

    @Insert
    suspend fun insert(daily: DailyEntity): Long

    @Delete
    suspend fun delete(daily: DailyEntity)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE catId = :catId ORDER BY dateEpochDay DESC, createdAtEpochMs DESC")
    fun observeForCat(catId: Long): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY dateEpochDay")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface HealthNoteDao {
    @Query("SELECT * FROM health_notes WHERE catId = :catId ORDER BY dateEpochDay DESC, createdAtEpochMs DESC")
    fun observeForCat(catId: Long): Flow<List<HealthNoteEntity>>

    @Query("SELECT * FROM health_notes ORDER BY dateEpochDay")
    suspend fun getAll(): List<HealthNoteEntity>

    @Insert
    suspend fun insert(note: HealthNoteEntity): Long

    @Delete
    suspend fun delete(note: HealthNoteEntity)
}

@Dao
interface CareTaskDao {
    /** 未来 [untilEpochDay]（含）之前仍待办的任务，供今日页与提醒使用 */
    @Query("SELECT * FROM care_tasks WHERE status = 'pending' AND dueDateEpochDay <= :untilEpochDay ORDER BY dueDateEpochDay")
    fun observeDueWithin(untilEpochDay: Long): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE status = 'pending' AND dueDateEpochDay <= :untilEpochDay ORDER BY dueDateEpochDay")
    suspend fun getDueWithin(untilEpochDay: Long): List<CareTaskEntity>

    @Insert
    suspend fun insertAll(tasks: List<CareTaskEntity>)

    @Insert
    suspend fun insert(task: CareTaskEntity): Long

    /** 删除某只猫的全部待办任务（生日变更时重建计划用） */
    @Query("DELETE FROM care_tasks WHERE catId = :catId AND status = 'pending'")
    suspend fun deletePendingForCat(catId: Long)

    /** 完成任务；周期任务由 UI 层按 intervalDays 生成下一轮再写入 */
    @Query("UPDATE care_tasks SET status = 'done', completedAtEpochMs = :completedAtEpochMs WHERE id = :taskId")
    suspend fun markDone(taskId: Long, completedAtEpochMs: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM care_tasks WHERE catId = :catId)")
    suspend fun hasTasksFor(catId: Long): Boolean

    @Query("SELECT * FROM care_tasks ORDER BY dueDateEpochDay")
    suspend fun getAll(): List<CareTaskEntity>
}
