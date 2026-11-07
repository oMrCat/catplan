package io.github.omrcat.catplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 猫咪档案 */
@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val breed: String? = null,
    /** "boy" / "girl"，v0.1 未做表单约束 */
    val gender: String? = null,
    /** 生日，存 epochDay，避免时区问题 */
    val birthdayEpochDay: Long,
    val neutered: Boolean = false,
    /** 头像 emoji，例如 🍊 */
    val avatarEmoji: String = "🐱",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

/** 体重记录：一只猫多条，构成体重曲线 */
@Entity(
    tableName = "weight_entries",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catId")],
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    /** 记录日期，epochDay */
    val dateEpochDay: Long,
    /** 单位：克（整数存储避免浮点误差，展示时 /1000） */
    val weightGrams: Int,
    val note: String? = null,
)

/** 成长相册：每张照片存为应用私有文件，DB 记录路径 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catId")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    /** 应用私有目录内的绝对路径 */
    val filePath: String,
    val note: String? = null,
    val dateEpochDay: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

/** 花费记录：catId 为空表示全家公摊 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catId"), Index("dateEpochDay")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 关联的猫咪；null = 全家公摊 */
    val catId: Long?,
    /** 金额，单位：分（整数避免浮点误差） */
    val amountCents: Long,
    /** 类目：主食 / 医疗 / 玩具 / 用品 / 其他 */
    val category: String,
    val note: String? = null,
    /** 花费日期，epochDay */
    val dateEpochDay: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
) {
    companion object {
        const val CATEGORY_FOOD = "主食"
        const val CATEGORY_MEDICAL = "医疗"
        const val CATEGORY_TOY = "玩具"
        const val CATEGORY_SUPPLIES = "用品"
        const val CATEGORY_OTHER = "其他"
        val CATEGORIES = listOf(CATEGORY_FOOD, CATEGORY_MEDICAL, CATEGORY_TOY, CATEGORY_SUPPLIES, CATEGORY_OTHER)
    }
}

/** 日常打卡：喂食（按猫或全家）与铲屎（全局） */
@Entity(
    tableName = "dailies",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catId"), Index("dateEpochDay")],
)
data class DailyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "feed" / "litter" */
    val type: String,
    /** 关联猫咪；null = 全家（铲屎为全局时恒为 null） */
    val catId: Long?,
    val dateEpochDay: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TYPE_FEED = "feed"
        const val TYPE_LITTER = "litter"
    }
}

/** 护理任务：疫苗 / 驱虫 / 体检，由 CareScheduleGenerator 生成 */
@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catId"), Index("dueDateEpochDay")],
)
data class CareTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    /** 任务类型，取值见 [TYPE_FVRCP] 等 */
    val type: String,
    val title: String,
    /** 到期日期，epochDay */
    val dueDateEpochDay: Long,
    /** 循环周期（天）；0 表示一次性任务 */
    val intervalDays: Int,
    /** pending / done / skipped */
    val status: String = STATUS_PENDING,
    val completedAtEpochMs: Long? = null,
) {
    companion object {
        const val TYPE_FVRCP = "vaccine_fvrcp"
        const val TYPE_RABIES = "vaccine_rabies"
        const val TYPE_DEWORM_IN = "deworm_in"
        const val TYPE_DEWORM_OUT = "deworm_out"
        const val TYPE_CHECKUP = "checkup"
        const val TYPE_BIRTHDAY = "birthday"

        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "done"
        const val STATUS_SKIPPED = "skipped"
    }
}
