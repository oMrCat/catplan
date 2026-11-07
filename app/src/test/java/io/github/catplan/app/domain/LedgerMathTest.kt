package io.github.catplan.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LedgerMathTest {

    private val day = LocalDate.of(2026, 11, 7).toEpochDay()
    private val otherMonth = LocalDate.of(2026, 10, 20).toEpochDay()

    private fun e(cents: Long, category: String, date: Long = day) =
        Expense(amountCents = cents, category = category, dateEpochDay = date)

    @Test
    fun `只统计当月花费`() {
        val summary = LedgerMath.summarize(
            listOf(
                e(100_00, "主食") to "橘子",
                e(50_00, "医疗") to "橘子",
                e(20_00, "玩具") to "年糕",
                e(88_00, "用品", otherMonth) to "橘子", // 上月，应排除
            ),
            2026, 11,
        )
        assertEquals(170_00, summary.totalCents)
    }

    @Test
    fun `类目降序排列`() {
        val summary = LedgerMath.summarize(
            listOf(
                e(10_00, "玩具") to "橘子",
                e(60_00, "主食") to "橘子",
                e(30_00, "医疗") to "年糕",
            ),
            2026, 11,
        )
        assertEquals(listOf("主食" to 60_00L, "医疗" to 30_00L, "玩具" to 10_00L), summary.byCategory)
    }

    @Test
    fun `按猫分摊且全家计入全家`() {
        val summary = LedgerMath.summarize(
            listOf(
                e(40_00, "医疗") to "橘子",
                e(60_00, "主食") to "全家",
            ),
            2026, 11,
        )
        assertEquals(listOf("全家" to 60_00L, "橘子" to 40_00L), summary.byCat)
    }

    @Test
    fun `金额格式化保留两位`() {
        assertEquals("¥12.05", LedgerMath.formatCents(1205))
        assertEquals("¥0.99", LedgerMath.formatCents(99))
    }

    @Test
    fun `占比计算`() {
        assertEquals(1.0f, LedgerMath.fraction(50, 50))
        assertEquals(0.5f, LedgerMath.fraction(25, 50))
        assertEquals(0.0f, LedgerMath.fraction(10, 0))
    }
}
