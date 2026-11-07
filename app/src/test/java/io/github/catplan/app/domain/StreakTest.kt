package io.github.catplan.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    private val today = 20758L

    @Test
    fun `空记录为0`() {
        assertEquals(0, Streak.streakDays(emptyList(), today))
    }

    @Test
    fun `今天有记录向下连续计数`() {
        assertEquals(3, Streak.streakDays(listOf(today, today - 1, today - 2, today - 10), today))
    }

    @Test
    fun `今天没记录从昨天起算`() {
        assertEquals(2, Streak.streakDays(listOf(today - 1, today - 2, today - 5), today))
    }

    @Test
    fun `昨天也没有则断档清零`() {
        assertEquals(0, Streak.streakDays(listOf(today - 2, today - 3), today))
    }

    @Test
    fun `重复日期去重`() {
        assertEquals(2, Streak.streakDays(listOf(today, today, today - 1, today - 1), today))
    }
}
