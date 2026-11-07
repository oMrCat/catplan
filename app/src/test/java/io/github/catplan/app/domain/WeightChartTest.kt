package io.github.catplan.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightChartTest {

    private val width = 300f
    private val height = 120f

    @Test
    fun `空数据返回空点位`() {
        val data = WeightChart.mapPoints(emptyList(), width, height)
        assertTrue(data.points.isEmpty())
    }

    @Test
    fun `单条数据居中`() {
        val data = WeightChart.mapPoints(listOf(100L to 4000), width, height)
        assertEquals(1, data.points.size)
        assertEquals(width / 2, data.points[0].x, 0.5f)
        assertEquals(height / 2, data.points[0].y, 0.5f)
        assertEquals(4000, data.minGrams)
        assertEquals(4000, data.maxGrams)
    }

    @Test
    fun `体重越大 y 越小`() {
        val data = WeightChart.mapPoints(
            listOf(100L to 3600, 130L to 4200),
            width, height,
        )
        assertEquals(2, data.points.size)
        assertTrue(data.points[0].y > data.points[1].y)
        // x 按日期升序
        assertTrue(data.points[0].x < data.points[1].x)
    }

    @Test
    fun `同一天多条只保留最后一条`() {
        val data = WeightChart.mapPoints(
            listOf(100L to 4000, 100L to 4100, 100L to 4200),
            width, height,
        )
        assertEquals(1, data.points.size)
        assertEquals(width / 2, data.points[0].x, 0.5f)
        assertEquals(height / 2, data.points[0].y, 0.5f)
        assertEquals(4200, data.maxGrams)
    }

    @Test
    fun `体重相同画平线`() {
        val data = WeightChart.mapPoints(
            listOf(100L to 4000, 130L to 4000, 160L to 4000),
            width, height,
        )
        data.points.forEach { p ->
            assertEquals(height / 2, p.y, 0.5f)
        }
        assertEquals(4000, data.minGrams)
        assertEquals(4000, data.maxGrams)
    }

    @Test
    fun `乱序输入自动排序`() {
        val data = WeightChart.mapPoints(
            listOf(160L to 4200, 100L to 3600),
            width, height,
        )
        assertEquals(2, data.points.size)
        assertTrue(data.points[0].x < data.points[1].x)
        assertTrue(data.points[0].y > data.points[1].y)
    }
}
