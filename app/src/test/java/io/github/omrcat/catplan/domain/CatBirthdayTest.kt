package io.github.omrcat.catplan.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CatBirthdayTest {

    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun `今年生日已过返回明年`() {
        assertEquals(LocalDate.of(2027, 1, 1), CatBirthday.nextBirthday(LocalDate.of(2024, 1, 1), today))
        assertEquals(55, CatBirthday.daysUntil(LocalDate.of(2024, 1, 1), today))
    }

    @Test
    fun `今天就是生日返回今天`() {
        assertEquals(today, CatBirthday.nextBirthday(LocalDate.of(2024, 11, 7), today))
        assertEquals(0, CatBirthday.daysUntil(LocalDate.of(2024, 11, 7), today))
    }

    @Test
    fun `明天生日`() {
        assertEquals(1, CatBirthday.daysUntil(LocalDate.of(2024, 11, 8), today))
    }

    @Test
    fun `2月29顺延为2月28`() {
        assertEquals(LocalDate.of(2027, 2, 28), CatBirthday.nextBirthday(LocalDate.of(2024, 2, 29), today))
    }

    @Test
    fun `闰年2月29正常识别`() {
        assertEquals(
            LocalDate.of(2028, 2, 29),
            CatBirthday.nextBirthday(LocalDate.of(2024, 2, 29), LocalDate.of(2028, 2, 1)),
        )
    }
}
