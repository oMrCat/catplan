package io.github.catplan.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CatAgeTest {

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun `两岁一个月`() {
        assertEquals("2 岁 1 个月", CatAge.describe(LocalDate.of(2024, 10, 6), today))
    }

    @Test
    fun `不满一岁按月计`() {
        assertEquals("6 个月", CatAge.describe(LocalDate.of(2026, 5, 6), today))
    }

    @Test
    fun `未满月`() {
        assertEquals("未满月", CatAge.describe(LocalDate.of(2026, 11, 1), today))
    }

    @Test
    fun `整岁不带月份`() {
        assertEquals("1 岁", CatAge.describe(LocalDate.of(2025, 11, 7), today))
    }

    @Test
    fun `生日在未来显示刚到家`() {
        assertEquals("刚到家", CatAge.describe(LocalDate.of(2026, 12, 1), today))
    }
}
