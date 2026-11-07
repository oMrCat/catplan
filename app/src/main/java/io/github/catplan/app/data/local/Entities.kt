package io.github.catplan.app.data.local

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
        // 任务类型（与 CareScheduleGenerator 的常量对应）
        const val TYPE_FVRCP = "vaccine_fvrcp"
        const val TYPE_RABIES = "vaccine_rabies"
        const val TYPE_DEWORM_IN = "deworm_in"
        const val TYPE_DEWORM_OUT = "deworm_out"
        const val TYPE_CHECKUP = "checkup"

        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "done"
        const val STATUS_SKIPPED = "skipped"
    }
}
