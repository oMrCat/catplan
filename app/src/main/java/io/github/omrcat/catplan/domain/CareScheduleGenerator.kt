package io.github.omrcat.catplan.domain

import java.time.LocalDate

/**
 * 按猫龄自动生成护理计划 —— CatPlan 的核心能力之一。
 * 填一次生日，即可排好未来一年的疫苗、驱虫和体检任务。
 *
 * 周期参考国内常见的养猫免疫/驱虫方案，仅作提醒用途，具体请遵医嘱：
 *  - 猫三联（FVRCP）：4 月龄内按 8 / 12 / 16 周做基础免疫，之后每年加强一次
 *  - 狂犬疫苗：每年一次
 *  - 体内驱虫：每 3 个月一次
 *  - 体外驱虫：每 1 个月一次
 *  - 体检：每年一次
 */
object CareScheduleGenerator {

    /** 一条待办任务的草稿，由 UI 层写入 care_tasks 表 */
    data class TaskDraft(
        val type: String,
        val title: String,
        val dueDate: LocalDate,
        /** 循环周期（天）；0 表示一次性任务 */
        val intervalDays: Int,
    )

    const val TYPE_FVRCP = "vaccine_fvrcp"
    const val TYPE_RABIES = "vaccine_rabies"
    const val TYPE_DEWORM_IN = "deworm_in"
    const val TYPE_DEWORM_OUT = "deworm_out"
    const val TYPE_CHECKUP = "checkup"

    private const val ANNUAL = 365
    private const val QUARTERLY = 90
    private const val MONTHLY = 30

    private val KITTEN_SHOTS = listOf(
        8 to "猫三联 · 基础免疫第 1 针",
        12 to "猫三联 · 基础免疫第 2 针",
        16 to "猫三联 · 基础免疫第 3 针",
    )

    /**
     * @param birthday    猫咪生日
     * @param baseline    排期基准日，通常传今天
     * @param lastFvrcp   上次猫三联时间（已知历史时传入，未知传 null）
     * @param lastRabies  上次狂犬时间
     * @param lastCheckup 上次体检时间
     * @param horizon     排期截止日，默认一年后
     */
    fun generate(
        birthday: LocalDate,
        baseline: LocalDate = LocalDate.now(),
        lastFvrcp: LocalDate? = null,
        lastRabies: LocalDate? = null,
        lastCheckup: LocalDate? = null,
        horizon: LocalDate = baseline.plusYears(1),
    ): List<TaskDraft> {
        val tasks = mutableListOf<TaskDraft>()
        val kittenSeriesEnd = birthday.plusWeeks(16)

        // —— 猫三联 ——
        if (lastFvrcp == null && baseline.isBefore(kittenSeriesEnd)) {
            // 幼猫：基础免疫三针；日期已落在基准日之前的顺延到基准日
            KITTEN_SHOTS.forEach { (week, title) ->
                tasks += TaskDraft(
                    type = TYPE_FVRCP,
                    title = title,
                    dueDate = laterOf(birthday.plusWeeks(week.toLong()), baseline),
                    intervalDays = 0,
                )
            }
            // 基础免疫完成一年后开始年度加强
            addRecurring(tasks, TYPE_FVRCP, "猫三联 · 年度加强", kittenSeriesEnd.plusYears(1), ANNUAL, horizon)
        } else {
            val anchor = lastFvrcp ?: baseline
            addRecurring(
                tasks, TYPE_FVRCP, "猫三联 · 年度加强",
                laterOf(anchor.plusYears(1), baseline), ANNUAL, horizon,
            )
        }

        // —— 狂犬：不知道上次时间就视为本次基准日到期 ——
        val rabiesAnchor = (lastRabies ?: baseline.minusDays(ANNUAL.toLong())).plusYears(1)
        addRecurring(tasks, TYPE_RABIES, "狂犬疫苗 · 年度", laterOf(rabiesAnchor, baseline), ANNUAL, horizon)

        // —— 驱虫：从基准日开始按周期滚动 ——
        addRecurring(tasks, TYPE_DEWORM_IN, "体内驱虫", baseline, QUARTERLY, horizon)
        addRecurring(tasks, TYPE_DEWORM_OUT, "体外驱虫", baseline, MONTHLY, horizon)

        // —— 体检 ——
        val checkupAnchor = (lastCheckup ?: baseline.minusDays(ANNUAL.toLong())).plusYears(1)
        addRecurring(tasks, TYPE_CHECKUP, "年度体检", laterOf(checkupAnchor, baseline), ANNUAL, horizon)

        return tasks.sortedBy { it.dueDate }
    }

    /** 从 [first] 开始按固定周期生成任务，直到 [horizon]（含） */
    private fun addRecurring(
        tasks: MutableList<TaskDraft>,
        type: String,
        title: String,
        first: LocalDate,
        intervalDays: Int,
        horizon: LocalDate,
    ) {
        var date = first
        while (!date.isAfter(horizon)) {
            tasks += TaskDraft(type, title, date, intervalDays)
            date = date.plusDays(intervalDays.toLong())
        }
    }

    private fun laterOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b
}
