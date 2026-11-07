package io.github.omrcat.catplan.domain

/**
 * 体重曲线映射：把（日期, 克）序列换算成图表坐标（纯函数，可单测）。
 * 坐标系 y 向下：体重越大 y 越小。
 */
object WeightChart {

    data class Point(val x: Float, val y: Float)

    data class ChartData(
        val points: List<Point>,
        val minGrams: Int,
        val maxGrams: Int,
    )

    /**
     * @param entries (dateEpochDay, grams)，乱序允许；同一天取最后一条
     * @param paddingH/paddingV 图表内边距
     */
    fun mapPoints(
        entries: List<Pair<Long, Int>>,
        width: Float,
        height: Float,
        paddingH: Float = 20f,
        paddingV: Float = 20f,
    ): ChartData {
        if (entries.isEmpty() || width <= 0f || height <= 0f) {
            return ChartData(emptyList(), 0, 0)
        }
        val series = entries
            .groupBy({ it.first }, { it.second })
            .map { (day, grams) -> day to grams.last() }
            .sortedBy { it.first }

        val minGrams = series.minOf { it.second }
        val maxGrams = series.maxOf { it.second }
        val dayMin = series.first().first
        val dayMax = series.last().first
        val spanDays = (dayMax - dayMin).coerceAtLeast(1)
        val flat = minGrams == maxGrams
        val gramSpan = (maxGrams - minGrams).coerceAtLeast(1)

        val usableW = (width - paddingH * 2).coerceAtLeast(0f)
        val usableH = (height - paddingV * 2).coerceAtLeast(0f)

        val points = series.mapIndexed { index, (day, grams) ->
            val fx = if (series.size == 1) 0.5f else (day - dayMin).toFloat() / spanDays
            val fy = if (flat) 0.5f else (grams - minGrams).toFloat() / gramSpan
            Point(
                x = paddingH + usableW * fx,
                y = paddingV + usableH * (1 - fy),
            )
        }
        return ChartData(points, minGrams, maxGrams)
    }
}
