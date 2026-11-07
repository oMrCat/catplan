package io.github.omrcat.catplan

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.omrcat.catplan.data.local.CareTaskEntity
import io.github.omrcat.catplan.data.local.CatEntity
import io.github.omrcat.catplan.data.local.CatPlanDatabase
import io.github.omrcat.catplan.data.local.ExpenseEntity
import io.github.omrcat.catplan.domain.Expense
import io.github.omrcat.catplan.domain.LedgerMath
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO 冒烟测试：多猫数据下的排序、聚合与级联删除（内存库）。
 */
@RunWith(AndroidJUnit4::class)
class DaoSmokeTest {

    private val db: CatPlanDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        CatPlanDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun multiCatOrderingAggregationAndCascade() = runBlocking {
        val names = listOf("阿甲", "阿乙", "阿丙")
        val ids = names.map { name ->
            db.catDao().insert(CatEntity(name = name, birthdayEpochDay = 19000))
        }
        ids.forEachIndexed { index, catId ->
            db.careTaskDao().insert(
                CareTaskEntity(
                    catId = catId,
                    type = CareTaskEntity.TYPE_DEWORM_OUT,
                    title = "体外驱虫 $index",
                    dueDateEpochDay = 20800L + index,
                    intervalDays = 30,
                ),
            )
            db.expenseDao().insert(
                ExpenseEntity(
                    catId = catId,
                    amountCents = 100L * (index + 1),
                    category = "主食",
                    dateEpochDay = 20758,
                ),
            )
        }

        // 到期任务按日期升序
        val due = db.careTaskDao().getDueWithin(20900)
        assertEquals(3, due.size)
        assertEquals(listOf(20800L, 20801L, 20802L), due.map { it.dueDateEpochDay })

        // 花费聚合（100+200+300=600 分）
        val summary = LedgerMath.summarize(
            db.expenseDao().getAll().map {
                Expense(it.amountCents, it.category, it.dateEpochDay) to (it.catId?.toString() ?: "全家")
            },
            2026, 11,
        )
        assertEquals(600L, summary.totalCents)

        // 级联删除：删猫后其花费一并消失
        val first = db.catDao().getById(ids[0])!!
        db.catDao().delete(first)
        assertEquals(2, db.expenseDao().getAll().size)
    }
}
