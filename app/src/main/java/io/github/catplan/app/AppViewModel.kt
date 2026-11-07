package io.github.catplan.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.catplan.app.data.BackupCodec
import io.github.catplan.app.data.PhotoStore
import io.github.catplan.app.data.WebDavClient
import io.github.catplan.app.data.local.CareTaskEntity
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.CatPlanDatabase
import io.github.catplan.app.data.local.DailyEntity
import io.github.catplan.app.data.local.ExpenseEntity
import io.github.catplan.app.data.local.PhotoEntity
import io.github.catplan.app.data.local.WeightEntryEntity
import io.github.catplan.app.domain.CatBirthday
import io.github.catplan.app.domain.CareScheduleGenerator
import io.github.catplan.app.domain.Streak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** UI 层展示用的任务 + 猫咪组合 */
data class CareTaskWithCat(
    val task: CareTaskEntity,
    val catName: String,
    val emoji: String,
) {
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(task.dueDateEpochDay)
}

/** UI 层展示用的花费 + 猫咪组合（catName 为「全家」表示公摊） */
data class ExpenseWithCat(
    val expense: ExpenseEntity,
    val catName: String,
    val emoji: String,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(expense.dateEpochDay)
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
    private val expenseDao = db.expenseDao()
    private val photoDao = db.photoDao()
    private val dailyDao = db.dailyDao()
    private val today: LocalDate = LocalDate.now()

    // —— WebDAV 配置（保存在本机 SharedPreferences）——

    private val webDavPrefs = getApplication<Application>().getSharedPreferences("webdav", Context.MODE_PRIVATE)

    var webDavUrl by mutableStateOf(webDavPrefs.getString("url", "") ?: "")
    var webDavUsername by mutableStateOf(webDavPrefs.getString("username", "") ?: "")
    var webDavPassword by mutableStateOf(webDavPrefs.getString("password", "") ?: "")

    val cats: StateFlow<List<CatEntity>> = catDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 今天（含已逾期）到未来 7 天的待办 + 3 天内的生日 */
    val upcomingTasks: StateFlow<List<CareTaskWithCat>> = combine(
        taskDao.observeDueWithin(today.plusDays(7).toEpochDay()),
        cats,
    ) { tasks, cats -> tasks.withCat(cats) + birthdayTasks(cats, withinDays = 7) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 未来 30 天的日历任务 + 30 天内的生日 */
    val calendarTasks: StateFlow<List<CareTaskWithCat>> = combine(
        taskDao.observeDueWithin(today.plusDays(30).toEpochDay()),
        cats,
    ) { tasks, cats -> tasks.withCat(cats) + birthdayTasks(cats, withinDays = 30) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 每只猫的最新体重（克） */
    val latestWeights: StateFlow<Map<Long, Int>> = weightDao.observeAll()
        .map { entries ->
            entries.groupBy { it.catId }
                .mapValues { (_, list) -> list.maxBy { it.dateEpochDay }.weightGrams }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** 花费记录（新→旧），供账本页统计与列表 */
    val expenses: StateFlow<List<ExpenseWithCat>> = combine(
        expenseDao.observeAll(),
        cats,
    ) { list, cats ->
        val byId = cats.associateBy { it.id }
        list.map { e ->
            val c = e.catId?.let { byId[it] }
            ExpenseWithCat(e, c?.name ?: "全家", c?.avatarEmoji ?: "🏠")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** 记一笔花费；catId 为 null 表示全家公摊 */
    fun addExpense(catId: Long?, amountCents: Long, category: String, note: String?) {
        viewModelScope.launch {
            expenseDao.insert(
                ExpenseEntity(
                    catId = catId,
                    amountCents = amountCents,
                    category = category,
                    note = note,
                    dateEpochDay = today.toEpochDay(),
                ),
            )
        }
    }

    // —— 成长相册 ——

    fun observePhotos(catId: Long) = photoDao.observeForCat(catId)

    fun observeWeights(catId: Long) = weightDao.observeForCat(catId)

    fun addPhoto(context: Context, catId: Long, uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { PhotoStore.copyToAlbum(context, uri, catId) }
            if (path == null) {
                onDone(false)
            } else {
                photoDao.insert(
                    PhotoEntity(catId = catId, filePath = path, dateEpochDay = today.toEpochDay()),
                )
                onDone(true)
            }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            PhotoStore.delete(photo.filePath)
            photoDao.delete(photo)
        }
    }

    // —— 备份 / 恢复（JSON）——

    private suspend fun collectBackup(): BackupCodec.BackupData = withContext(Dispatchers.IO) {
        BackupCodec.BackupData(
            cats = catDao.getAll(),
            weights = weightDao.getAll(),
            careTasks = taskDao.getAll(),
            expenses = expenseDao.getAll(),
        )
    }

    /** 按「名字 + 生日」合并猫咪，其余记录追加；返回摘要文本 */
    private suspend fun applyRestore(json: String): String = withContext(Dispatchers.IO) {
        val data = BackupCodec.decode(json)
        val existing = catDao.getAll()
        val idMap = mutableMapOf<Long, Long>()
        var addedCats = 0
        data.cats.forEach { c ->
            val match = existing.find { it.name == c.name && it.birthdayEpochDay == c.birthdayEpochDay }
            val newId = match?.id ?: catDao.insert(c.copy(id = 0))
            if (match == null) addedCats++
            idMap[c.id] = newId
        }
        data.weights.forEach { w ->
            val mapped = idMap[w.catId] ?: return@forEach
            weightDao.insert(w.copy(id = 0, catId = mapped))
        }
        var addedTasks = 0
        data.careTasks.forEach { t ->
            val mapped = idMap[t.catId] ?: return@forEach
            taskDao.insert(t.copy(id = 0, catId = mapped))
            addedTasks++
        }
        var addedExpenses = 0
        data.expenses.forEach { e ->
            val mapped = if (e.catId == null) null else idMap[e.catId] ?: return@forEach
            expenseDao.insert(e.copy(id = 0, catId = mapped))
            addedExpenses++
        }
        "已恢复 $addedCats 只新猫、${data.weights.size} 条体重、$addedTasks 项任务、$addedExpenses 笔花费"
    }

    fun exportTo(context: Context, uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val data = collectBackup()
                val json = BackupCodec.encode(data)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入所选位置")
                }
                "已导出 ${data.cats.size} 只猫、${data.weights.size} 条体重、${data.careTasks.size} 项任务、${data.expenses.size} 笔花费"
            }.onSuccess(onDone).onFailure { onDone("导出失败：${it.message}") }
        }
    }

    fun restoreFrom(context: Context, uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("无法读取所选文件")
                }
                applyRestore(json)
            }.onSuccess(onDone).onFailure { onDone("恢复失败：${it.message}") }
        }
    }

    // —— WebDAV 同步（家庭共享基础版）——

    fun saveWebDavConfig() {
        webDavPrefs.edit()
            .putString("url", webDavUrl.trim())
            .putString("username", webDavUsername.trim())
            .putString("password", webDavPassword)
            .apply()
    }

    fun uploadToWebDav(onResult: (String) -> Unit) {
        val url = webDavUrl.trim()
        if (url.isBlank()) {
            onResult("请先填写 WebDAV 文件地址")
            return
        }
        saveWebDavConfig()
        viewModelScope.launch {
            runCatching {
                val json = BackupCodec.encode(collectBackup())
                withContext(Dispatchers.IO) {
                    WebDavClient.upload(url, webDavUsername.trim(), webDavPassword, json).getOrThrow()
                }
                "已上传备份 ✅"
            }.onSuccess(onResult).onFailure { onResult("上传失败：${it.message}") }
        }
    }

    fun restoreFromWebDav(onResult: (String) -> Unit) {
        val url = webDavUrl.trim()
        if (url.isBlank()) {
            onResult("请先填写 WebDAV 文件地址")
            return
        }
        saveWebDavConfig()
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    WebDavClient.download(url, webDavUsername.trim(), webDavPassword).getOrThrow()
                }
                applyRestore(json)
            }.onSuccess(onResult).onFailure { onResult("下载/恢复失败：${it.message}") }
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

    /** 连续记录天数（打卡 + 体重） */
    val streak: StateFlow<Int> = combine(
        dailyDao.observeSince(0),
        weightDao.observeAll(),
    ) { dailies, weights ->
        Streak.streakDays(
            dailies.map { it.dateEpochDay } + weights.map { it.dateEpochDay },
            today.toEpochDay(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 今天的打卡记录 */
    val todayDailies: StateFlow<List<DailyEntity>> = dailyDao.observeSince(today.toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 打卡：type = feed（catId 可为 null=全家）/ litter（catId 恒为 null） */
    fun addDaily(type: String, catId: Long?) {
        viewModelScope.launch {
            dailyDao.insert(DailyEntity(type = type, catId = catId, dateEpochDay = today.toEpochDay()))
        }
    }

    /** 生日伪任务：从猫咪生日实时推导，不入库 */
    private fun birthdayTasks(cats: List<CatEntity>, withinDays: Long): List<CareTaskWithCat> =
        cats.mapNotNull { cat ->
            val next = CatBirthday.nextBirthday(LocalDate.ofEpochDay(cat.birthdayEpochDay), today)
            val days = ChronoUnit.DAYS.between(today, next)
            if (days <= withinDays) {
                CareTaskWithCat(
                    task = CareTaskEntity(
                        catId = cat.id,
                        type = CareTaskEntity.TYPE_BIRTHDAY,
                        title = "🎂 ${cat.name} 的生日",
                        dueDateEpochDay = next.toEpochDay(),
                        intervalDays = 365,
                    ),
                    catName = cat.name,
                    emoji = cat.avatarEmoji,
                )
            } else null
        }

    private fun List<CareTaskEntity>.withCat(cats: List<CatEntity>): List<CareTaskWithCat> {
        val byId = cats.associateBy { it.id }
        return map { t ->
            val c = byId[t.catId]
            CareTaskWithCat(t, c?.name ?: "猫咪", c?.avatarEmoji ?: "🐱")
        }
    }
}
