package io.github.omrcat.catplan.domain

import java.time.LocalDate

/** 账本统计：纯函数，方便单元测试 */
object LedgerMath {

    data class Summary(
        val totalCents: Long,
        /** 类目 → 金额，降序 */
        val byCategory: List<Pair<String, Long>>,
        /** 猫咪（含「全家」）→ 金额，降序 */
        val byCat: List<Pair<String, Long>>,
    )

    fun summarize(
        expenses: List<Pair<Expense, String>>,
        year: Int,
        month: Int,
    ): Summary {
        val inMonth = expenses.filter { (e, _) ->
            val d = LocalDate.ofEpochDay(e.dateEpochDay)
            d.year == year && d.monthValue == month
        }

        val total = inMonth.sumOf { it.first.amountCents }
        val byCategory = inMonth
            .groupBy { it.first.category }
            .map { (k, v) -> k to v.sumOf { it.first.amountCents } }
            .sortedByDescending { it.second }
        val byCat = inMonth
            .groupBy { it.second }
            .map { (k, v) -> k to v.sumOf { it.first.amountCents } }
            .sortedByDescending { it.second }

        return Summary(total, byCategory, byCat)
    }

    fun formatCents(cents: Long): String = "¥${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

    /** 供 UI 绘制条形图用的占比（0..1） */
    fun fraction(value: Long, max: Long): Float =
        if (max <= 0) 0f else value.toFloat() / max.toFloat()
}

/** 统计用的轻量快照，避免直接依赖 Room 实体 */
data class Expense(
    val amountCents: Long,
    val category: String,
    val dateEpochDay: Long,
)
