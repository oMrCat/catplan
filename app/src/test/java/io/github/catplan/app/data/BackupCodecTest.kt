package io.github.catplan.app.data

import io.github.catplan.app.data.local.CareTaskEntity
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.DailyEntity
import io.github.catplan.app.data.local.ExpenseEntity
import io.github.catplan.app.data.local.WeightEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 备份编解码往返测试（Robolectric：org.json 需要 Android 运行时）。
 * 数据安全关键路径，不允许静默丢字段。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class) // 纯 JSON 编解码，无需真实 Application/WorkManager
class BackupCodecTest {

    private fun sample() = BackupCodec.BackupData(
        cats = listOf(
            CatEntity(id = 1, name = "橘子", breed = null, gender = "boy", birthdayEpochDay = 19723, neutered = true, avatarEmoji = "🍊", createdAtEpochMs = 1000),
            CatEntity(id = 2, name = "年糕", breed = "英短", gender = null, birthdayEpochDay = 20300, neutered = false, avatarEmoji = "🍡", createdAtEpochMs = 2000),
        ),
        weights = listOf(WeightEntryEntity(catId = 1, dateEpochDay = 20700, weightGrams = 4100, note = null)),
        careTasks = listOf(
            CareTaskEntity(catId = 1, type = CareTaskEntity.TYPE_FVRCP, title = "猫三联 · 年度加强", dueDateEpochDay = 20800, intervalDays = 365),
        ),
        expenses = listOf(
            ExpenseEntity(catId = null, amountCents = 12850, category = "主食", note = null, dateEpochDay = 20758),
            ExpenseEntity(catId = 2, amountCents = 4500, category = "医疗", note = "疫苗", dateEpochDay = 20758),
        ),
        dailies = listOf(
            DailyEntity(type = DailyEntity.TYPE_FEED, catId = 1, dateEpochDay = 20758),
            DailyEntity(type = DailyEntity.TYPE_LITTER, catId = null, dateEpochDay = 20758),
        ),
    )

    @Test
    fun `往返编解码保持全部字段`() {
        val original = sample()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original.cats, decoded.cats)
        assertEquals(original.weights, decoded.weights)
        assertEquals(original.careTasks, decoded.careTasks)
        assertEquals(original.expenses, decoded.expenses)
        assertEquals(original.dailies, decoded.dailies)
    }

    @Test
    fun `可空字段为null时解码还原为null而不是null字符串`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample()))
        // 断言消息携带实际值：Windows 本地绿 / Linux CI 挂的平台差异取证
        assertTrue(
            "cats[0].breed=${decoded.cats[0].breed}",
            decoded.cats[0].breed == null,
        )
        assertTrue(
            "cats[1].gender=${decoded.cats[1].gender}",
            decoded.cats[1].gender == null,
        )
        assertTrue(
            "expenses[0].catId=${decoded.expenses[0].catId}",
            decoded.expenses[0].catId == null,
        )
        assertTrue(
            "dailies[1].catId=${decoded.dailies[1].catId}",
            decoded.dailies[1].catId == null,
        )
    }

    @Test
    fun `缺少可空字段时按默认值解码`() {
        val minimal = """
            {"app":"catplan","cats":[{"id":1,"name":"小白","birthdayEpochDay":20000}]}
        """.trimIndent()
        val decoded = BackupCodec.decode(minimal)
        assertEquals(1, decoded.cats.size)
        assertEquals("小白", decoded.cats[0].name)
        assertNull(decoded.cats[0].breed)
        assertEquals("🐱", decoded.cats[0].avatarEmoji)
        assertTrue(decoded.weights.isEmpty())
        assertTrue(decoded.dailies.isEmpty())
    }

    @Test
    fun `恶意截断JSON抛异常而不是静默成功`() {
        val truncated = BackupCodec.encode(sample()).dropLast(20)
        var thrown = false
        try {
            BackupCodec.decode(truncated)
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
