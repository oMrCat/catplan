package io.github.omrcat.catplan.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omrcat.catplan.AppViewModel
import java.time.LocalDate

/**
 * 健康日历：未来 30 天的护理任务，按到期日排列。
 * 数据由添加猫咪时 CareScheduleGenerator 自动生成。
 */
@Composable
fun CalendarScreen(vm: AppViewModel = viewModel(), modifier: Modifier = Modifier) {
    val tasks by vm.calendarTasks.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("健康日历", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${today.year}年${today.monthValue}月 · 未来 30 天",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("即将到来", style = MaterialTheme.typography.titleMedium)
                if (tasks.isEmpty()) {
                    Text(
                        "近 30 天没有安排。添加猫咪后，护理计划会按生日自动生成 ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tasks.forEach { item ->
                        EventRow(
                            date = dayLabel(item.dueDate, today),
                            title = "${item.catName} · ${item.task.title}",
                            subtitle = intervalText(item.task.intervalDays),
                        )
                    }
                }
            }
        }

        Text(
            "提醒按猫龄与免疫周期自动生成，到期前 3 天本地推送。",
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

private fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "今天"
    today.plusDays(1) -> "明天"
    else -> "${d.monthValue}月${d.dayOfMonth}日"
}

private fun intervalText(days: Int): String = when (days) {
    0 -> "一次性"
    30 -> "每 1 个月"
    90 -> "每 3 个月"
    365 -> "每年"
    else -> "每 $days 天"
}
