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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omrcat.catplan.AppViewModel
import io.github.omrcat.catplan.R
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
        Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.calendar_period, today.year, today.monthValue),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.calendar_upcoming), style = MaterialTheme.typography.titleMedium)
                if (tasks.isEmpty()) {
                    Text(
                        stringResource(R.string.calendar_empty),
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
            stringResource(R.string.calendar_note),
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

@Composable
private fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> stringResource(R.string.day_today)
    today.plusDays(1) -> stringResource(R.string.day_tomorrow)
    else -> stringResource(R.string.date_short, d.monthValue, d.dayOfMonth)
}

@Composable
private fun intervalText(days: Int): String = when (days) {
    0 -> stringResource(R.string.interval_once)
    30 -> stringResource(R.string.interval_monthly)
    90 -> stringResource(R.string.interval_quarterly)
    365 -> stringResource(R.string.interval_yearly)
    else -> stringResource(R.string.interval_days, days)
}
