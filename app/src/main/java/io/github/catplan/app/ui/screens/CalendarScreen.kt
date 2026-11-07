package io.github.catplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 健康日历：按月展示全部猫咪的护理任务。
 *
 * TODO(v0.1)：读取 care_tasks 表按月分组展示，并实现顶部的月历控件；
 * 数据由 CareScheduleGenerator 在添加猫咪时生成。
 */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("健康日历", style = MaterialTheme.typography.headlineSmall)
        Text(
            "2026年11月",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("即将到来", style = MaterialTheme.typography.titleMedium)
                EventRow("今天", "橘子 · 体外驱虫", "11月6日 · 每 1 个月")
                EventRow("11-13", "年糕 · 猫三联加强", "年度免疫 · 提前 3 天已推送")
                EventRow("11-20", "煤球 · 年度体检", "血常规 + 粪检")
                EventRow("11-27", "全员 · 体内驱虫", "每 3 个月一次")
            }
        }

        Text(
            "提醒会按猫龄与免疫周期自动生成，提前 3 天推送。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventRow(date: String, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                date,
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
