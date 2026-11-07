package io.github.omrcat.catplan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 护理排期引擎的单元测试：纯 JVM 运行，不依赖 Android 设备。
 */
class CareScheduleGeneratorTest {

    @Test
    fun `幼猫按8周12周16周排基础免疫且一年内不含加强针`() {
        val birthday = LocalDate.of(2026, 10, 1)
        val baseline = LocalDate.of(2026, 11, 6)
        val horizon = baseline.plusYears(1)

        val plan = CareScheduleGenerator.generate(birthday, baseline, horizon = horizon)
        val fvrcp = plan.filter { it.type == CareScheduleGenerator.TYPE_FVRCP }

        assertEquals(3, fvrcp.size)
        assertEquals(LocalDate.of(2026, 11, 26), fvrcp[0].dueDate) // 8 周
        assertEquals(LocalDate.of(2026, 12, 24), fvrcp[1].dueDate) // 12 周
        assertEquals(LocalDate.of(2027, 1, 21), fvrcp[2].dueDate) // 16 周
        assertTrue(fvrcp.all { it.intervalDays == 0 })
    }

    @Test
    fun `成猫按年度加强三联`() {
        val birthday = LocalDate.of(2022, 1, 1)
        val baseline = LocalDate.of(2026, 11, 6)
        val horizon = baseline.plusYears(1)

        val plan = CareScheduleGenerator.generate(birthday, baseline, horizon = horizon)
        val fvrcp = plan.filter { it.type == CareScheduleGenerator.TYPE_FVRCP }

        assertEquals(1, fvrcp.size)
        assertEquals(baseline.plusDays(365), fvrcp[0].dueDate)
    }

    @Test
    fun `已知上次免疫时间时以它为锚点排加强针`() {
        val birthday = LocalDate.of(2022, 1, 1)
        val baseline = LocalDate.of(2026, 11, 6)
        val lastFvrcp = LocalDate.of(2026, 6, 1)

        val plan = CareScheduleGenerator.generate(birthday, baseline, lastFvrcp = lastFvrcp)
        val fvrcp = plan.filter { it.type == CareScheduleGenerator.TYPE_FVRCP }

        assertEquals(1, fvrcp.size)
        assertEquals(LocalDate.of(2027, 6, 1), fvrcp[0].dueDate)
    }

    @Test
    fun `驱虫按周期滚动且数量正确`() {
        val birthday = LocalDate.of(2022, 1, 1)
        val baseline = LocalDate.of(2026, 11, 6)
        val horizon = baseline.plusYears(1)

        val plan = CareScheduleGenerator.generate(birthday, baseline, horizon = horizon)

        // 体外每 30 天：一年 13 次；体内每 90 天：一年 5 次
        assertEquals(13, plan.count { it.type == CareScheduleGenerator.TYPE_DEWORM_OUT })
        assertEquals(5, plan.count { it.type == CareScheduleGenerator.TYPE_DEWORM_IN })
        assertTrue(plan.all { it.type != CareScheduleGenerator.TYPE_DEWORM_OUT || it.dueDate <= horizon })
    }

    @Test
    fun `狂犬与体检每年一次且包含截止日当天`() {
        val birthday = LocalDate.of(2022, 1, 1)
        val baseline = LocalDate.of(2026, 11, 6)
        val horizon = baseline.plusYears(1)

        val plan = CareScheduleGenerator.generate(birthday, baseline, horizon = horizon)

        // 首次落在基准日，第二次恰好是 horizon 当天（含端点）
        assertEquals(2, plan.count { it.type == CareScheduleGenerator.TYPE_RABIES })
        assertEquals(2, plan.count { it.type == CareScheduleGenerator.TYPE_CHECKUP })
        assertTrue(plan.any {
            it.type == CareScheduleGenerator.TYPE_RABIES && it.dueDate == baseline
        })
        assertTrue(plan.any {
            it.type == CareScheduleGenerator.TYPE_RABIES && it.dueDate == horizon
        })
    }

    @Test
    fun `整份计划按到期日升序排列`() {
        val birthday = LocalDate.of(2026, 10, 1)
        val baseline = LocalDate.of(2026, 11, 6)

        val plan = CareScheduleGenerator.generate(birthday, baseline)

        assertEquals(plan, plan.sortedBy { it.dueDate })
    }

    @Test
    fun `截止期早于后续任务时只保留视野内的任务`() {
        val birthday = LocalDate.of(2022, 1, 1)
        val baseline = LocalDate.of(2026, 11, 6)

        val plan = CareScheduleGenerator.generate(
            birthday,
            baseline,
            horizon = baseline.plusDays(10),
        )

        // 猫三联年度加强在一年后，10 天视野内不应出现
        assertTrue(plan.none { it.type == CareScheduleGenerator.TYPE_FVRCP })
        // 首次驱虫/狂犬/体检都落在基准日当天，各 1 条
        assertEquals(1, plan.count { it.type == CareScheduleGenerator.TYPE_DEWORM_OUT })
        assertEquals(1, plan.count { it.type == CareScheduleGenerator.TYPE_RABIES })
        assertEquals(1, plan.count { it.type == CareScheduleGenerator.TYPE_CHECKUP })
        assertTrue(plan.all { !it.dueDate.isBefore(baseline) && !it.dueDate.isAfter(baseline.plusDays(10)) })
    }
}
