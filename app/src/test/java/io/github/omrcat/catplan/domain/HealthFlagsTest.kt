package io.github.omrcat.catplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthFlagsTest {

    @Test
    fun `全部正常无标记`() {
        val flags = HealthFlags.abnormalFlags("好", "活泼", "正常", "正常")
        assertTrue(flags.isEmpty())
        assertFalse(HealthFlags.urgent(flags))
    }

    @Test
    fun `软便与萎靡被标记`() {
        val flags = HealthFlags.abnormalFlags("好", "萎靡", "软便", "正常")
        assertEquals(listOf("精神 萎靡", "便便 软便"), flags)
        assertFalse(HealthFlags.urgent(flags))
    }

    @Test
    fun `血尿属就医级异常`() {
        val flags = HealthFlags.abnormalFlags(null, null, "正常", "血尿")
        assertEquals(listOf("尿液 血尿"), flags)
        assertTrue(HealthFlags.urgent(flags))
    }

    @Test
    fun `便血也属就医级异常`() {
        val flags = HealthFlags.abnormalFlags(null, null, "带血", null)
        assertTrue(HealthFlags.urgent(flags))
    }

    @Test
    fun `未记录字段不产生标记`() {
        val flags = HealthFlags.abnormalFlags(null, null, null, null)
        assertTrue(flags.isEmpty())
    }
}
