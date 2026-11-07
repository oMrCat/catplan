package io.github.catplan.app.data

import io.github.catplan.app.data.local.CareTaskEntity
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.ExpenseEntity
import io.github.catplan.app.data.local.WeightEntryEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份编解码：JSON（org.json，Android 内置，零依赖）。
 * 注意：相册照片是二进制文件，不参与 JSON 备份。
 */
object BackupCodec {

    data class BackupData(
        val cats: List<CatEntity>,
        val weights: List<WeightEntryEntity>,
        val careTasks: List<CareTaskEntity>,
        val expenses: List<ExpenseEntity>,
    )

    fun encode(d: BackupData): String {
        val root = JSONObject()
            .put("app", "catplan")
            .put("schemaVersion", 2)
            .put("exportedAt", System.currentTimeMillis())

        root.put(
            "cats",
            JSONArray().apply {
                d.cats.forEach { c ->
                    put(
                        JSONObject()
                            .put("id", c.id)
                            .put("name", c.name)
                            .put("breed", c.breed ?: JSONObject.NULL)
                            .put("gender", c.gender ?: JSONObject.NULL)
                            .put("birthdayEpochDay", c.birthdayEpochDay)
                            .put("neutered", c.neutered)
                            .put("avatarEmoji", c.avatarEmoji)
                            .put("createdAtEpochMs", c.createdAtEpochMs),
                    )
                }
            },
        )
        root.put(
            "weights",
            JSONArray().apply {
                d.weights.forEach { w ->
                    put(
                        JSONObject()
                            .put("catId", w.catId)
                            .put("dateEpochDay", w.dateEpochDay)
                            .put("weightGrams", w.weightGrams)
                            .put("note", w.note ?: JSONObject.NULL),
                    )
                }
            },
        )
        root.put(
            "careTasks",
            JSONArray().apply {
                d.careTasks.forEach { t ->
                    put(
                        JSONObject()
                            .put("catId", t.catId)
                            .put("type", t.type)
                            .put("title", t.title)
                            .put("dueDateEpochDay", t.dueDateEpochDay)
                            .put("intervalDays", t.intervalDays)
                            .put("status", t.status)
                            .put("completedAtEpochMs", t.completedAtEpochMs ?: JSONObject.NULL),
                    )
                }
            },
        )
        root.put(
            "expenses",
            JSONArray().apply {
                d.expenses.forEach { e ->
                    put(
                        JSONObject()
                            .put("catId", e.catId ?: JSONObject.NULL)
                            .put("amountCents", e.amountCents)
                            .put("category", e.category)
                            .put("note", e.note ?: JSONObject.NULL)
                            .put("dateEpochDay", e.dateEpochDay),
                    )
                }
            },
        )
        return root.toString(2)
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        val cats = mutableListOf<CatEntity>()
        val weights = mutableListOf<WeightEntryEntity>()
        val tasks = mutableListOf<CareTaskEntity>()
        val expenses = mutableListOf<ExpenseEntity>()

        root.optJSONArray("cats")?.forEach { c ->
            cats += CatEntity(
                id = c.getLong("id"),
                name = c.getString("name"),
                breed = if (c.isNull("breed")) null else c.optString("breed"),
                gender = if (c.isNull("gender")) null else c.optString("gender"),
                birthdayEpochDay = c.getLong("birthdayEpochDay"),
                neutered = c.optBoolean("neutered", false),
                avatarEmoji = c.optString("avatarEmoji", "🐱"),
                createdAtEpochMs = c.optLong("createdAtEpochMs", System.currentTimeMillis()),
            )
        }
        root.optJSONArray("weights")?.forEach { w ->
            weights += WeightEntryEntity(
                catId = w.getLong("catId"),
                dateEpochDay = w.getLong("dateEpochDay"),
                weightGrams = w.getInt("weightGrams"),
                note = if (w.isNull("note")) null else w.optString("note"),
            )
        }
        root.optJSONArray("careTasks")?.forEach { t ->
            tasks += CareTaskEntity(
                catId = t.getLong("catId"),
                type = t.getString("type"),
                title = t.getString("title"),
                dueDateEpochDay = t.getLong("dueDateEpochDay"),
                intervalDays = t.getInt("intervalDays"),
                status = t.optString("status", CareTaskEntity.STATUS_PENDING),
                completedAtEpochMs = if (t.isNull("completedAtEpochMs")) null else t.optLong("completedAtEpochMs"),
            )
        }
        root.optJSONArray("expenses")?.forEach { e ->
            expenses += ExpenseEntity(
                catId = if (e.isNull("catId")) null else e.getLong("catId"),
                amountCents = e.getLong("amountCents"),
                category = e.getString("category"),
                note = if (e.isNull("note")) null else e.optString("note"),
                dateEpochDay = e.getLong("dateEpochDay"),
            )
        }
        return BackupData(cats, weights, tasks, expenses)
    }

    private inline fun JSONArray.forEach(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) action(getJSONObject(i))
    }
}
