package io.github.catplan.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.catplan.app.data.local.CareTaskEntity
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.CatPlanDatabase
import io.github.catplan.app.data.local.WeightEntryEntity
import io.github.catplan.app.domain.CareScheduleGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** UI 层展示用的任务 + 猫咪组合 */
data class CareTaskWithCat(
    val task: CareTaskEntity,
    val catName: String,
    val emoji: String,
) {
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(task.dueDateEpochDay)
}

/**
 * 全应用共享的状态源：观察数据库、写入猫咪/体重/任务。
 * v0.1 规模下直接持有 DAO；后续模块化时再拆 Repository。
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = CatPlanDatabase.get(app)
    private val catDao = db.catDao()
    private val taskDao = db.careTaskDao()
    private val weightDao = db.weightDao()
    private val today: LocalDate = LocalDate.now()

    val cats: StateFlow<List<CatEntity>> = catDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 今天（含已逾期）到未来 7 天的待办 */
    val upcomingTasks: StateFlow<List<CareTaskWithCat>> = combine(
        taskDao.observeDueWithin(today.plusDays(7).toEpochDay()),
        cats,
    ) { tasks, cats -> tasks.withCat(cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 未来 30 天的日历任务 */
    val calendarTasks: StateFlow<List<CareTaskWithCat>> = combine(
        taskDao.observeDueWithin(today.plusDays(30).toEpochDay()),
        cats,
    ) { tasks, cats -> tasks.withCat(cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 每只猫的最新体重（克） */
    val latestWeights: StateFlow<Map<Long, Int>> = weightDao.observeAll()
        .map { entries ->
            entries.groupBy { it.catId }
                .mapValues { (_, list) -> list.maxBy { it.dateEpochDay }.weightGrams }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** 添加猫咪并按生日自动生成一年的护理计划 */
    fun addCat(
        name: String,
        breed: String?,
        gender: String?,
        birthday: LocalDate,
        neutered: Boolean,
        emoji: String,
        onDone: (Int) -> Unit,
    ) {
        viewModelScope.launch {
            val catId = catDao.insert(
                CatEntity(
                    name = name,
                    breed = breed?.takeIf { it.isNotBlank() },
                    gender = gender,
                    birthdayEpochDay = birthday.toEpochDay(),
                    neutered = neutered,
                    avatarEmoji = emoji,
                ),
            )
            val drafts = CareScheduleGenerator.generate(birthday, LocalDate.now())
            taskDao.insertAll(
                drafts.map { d ->
                    CareTaskEntity(
                        catId = catId,
                        type = d.type,
                        title = d.title,
                        dueDateEpochDay = d.dueDate.toEpochDay(),
                        intervalDays = d.intervalDays,
                    )
                },
            )
            onDone(drafts.size)
        }
    }

    fun recordWeight(catId: Long, grams: Int) {
        viewModelScope.launch {
            weightDao.insert(
                WeightEntryEntity(catId = catId, dateEpochDay = today.toEpochDay(), weightGrams = grams),
            )
        }
    }

    /** 完成任务；循环任务按周期滚动生成下一轮 */
    fun completeTask(item: CareTaskWithCat) {
        viewModelScope.launch {
            taskDao.markDone(item.task.id, System.currentTimeMillis())
            if (item.task.intervalDays > 0) {
                taskDao.insert(
                    item.task.copy(
                        id = 0,
                        dueDateEpochDay = item.task.dueDateEpochDay + item.task.intervalDays,
                        status = CareTaskEntity.STATUS_PENDING,
                        completedAtEpochMs = null,
                    ),
                )
            }
        }
    }

    private fun List<CareTaskEntity>.withCat(cats: List<CatEntity>): List<CareTaskWithCat> {
        val byId = cats.associateBy { it.id }
        return map { t ->
            val c = byId[t.catId]
            CareTaskWithCat(t, c?.name ?: "猫咪", c?.avatarEmoji ?: "🐱")
        }
    }
}
