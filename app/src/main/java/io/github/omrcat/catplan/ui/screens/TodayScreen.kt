package io.github.omrcat.catplan.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omrcat.catplan.AppViewModel
import io.github.omrcat.catplan.R
import io.github.omrcat.catplan.data.local.CatEntity
import io.github.omrcat.catplan.data.local.DailyEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 今日页：问候（真实连续天数）+ 真实待办卡片 + 日常打卡。
 * 待办来自 CareTaskDao（未来 7 天含逾期）与生日推导；「完成」会把周期任务滚动到下一轮。
 */
@Composable
fun TodayScreen(vm: AppViewModel = viewModel(), modifier: Modifier = Modifier) {
    val tasks by vm.upcomingTasks.collectAsStateWithLifecycle()
    val cats by vm.cats.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val dailies by vm.todayDailies.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWeightDialog by remember { mutableStateOf(false) }
    var showFeedDialog by remember { mutableStateOf(false) }
    var undoType by remember { mutableStateOf<String?>(null) }

    val fedToday = dailies.any { it.type == DailyEntity.TYPE_FEED }
    val litterToday = dailies.any { it.type == DailyEntity.TYPE_LITTER }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.today_greeting), style = MaterialTheme.typography.headlineSmall)
        Text(
            if (tasks.isEmpty()) {
                stringResource(R.string.today_streak_none, streak)
            } else {
                stringResource(R.string.today_streak_tasks, streak, tasks.size)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.today_section_tasks), style = MaterialTheme.typography.titleMedium)
                if (tasks.isEmpty()) {
                    Text(
                        stringResource(R.string.today_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tasks.forEach { item ->
                        TaskRow(
                            emoji = item.emoji,
                            title = "${item.catName} · ${item.task.title}",
                            subtitle = dueText(item.dueDate),
                            action = stringResource(R.string.today_action_done),
                            onAction = { vm.completeTask(item) },
                        )
                    }
                }
            }
        }

        Text(stringResource(R.string.today_section_daily), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { showWeightDialog = true }, label = { Text(stringResource(R.string.daily_weight)) })
            AssistChip(
                onClick = {
                    if (fedToday) undoType = DailyEntity.TYPE_FEED else showFeedDialog = true
                },
                label = { Text(stringResource(if (fedToday) R.string.daily_fed else R.string.daily_feed)) },
            )
            AssistChip(
                onClick = {
                    if (litterToday) {
                        undoType = DailyEntity.TYPE_LITTER
                    } else {
                        vm.addDaily(DailyEntity.TYPE_LITTER, catId = null)
                        Toast.makeText(context, context.getString(R.string.toast_litter_done), Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text(stringResource(if (litterToday) R.string.daily_littered else R.string.daily_litter)) },
            )
            AssistChip(
                onClick = { Toast.makeText(context, context.getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show() },
                label = { Text(stringResource(R.string.daily_diary)) },
            )
        }
    }

    if (showWeightDialog) {
        WeightRecordDialog(
            cats = cats,
            onDismiss = { showWeightDialog = false },
            onSave = { catId, grams ->
                showWeightDialog = false
                vm.recordWeight(catId, grams)
                Toast.makeText(context, context.getString(R.string.toast_weight_saved), Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showFeedDialog) {
        FeedDailyDialog(
            cats = cats,
            onDismiss = { showFeedDialog = false },
            onSave = { catId ->
                showFeedDialog = false
                vm.addDaily(DailyEntity.TYPE_FEED, catId)
                Toast.makeText(context, context.getString(R.string.toast_feed_done), Toast.LENGTH_SHORT).show()
            },
        )
    }

    undoType?.let { type ->
        UndoDailyDialog(
            vm = vm,
            type = type,
            cats = cats,
            onDismiss = { undoType = null },
            onUndo = { daily ->
                vm.removeDaily(daily)
                Toast.makeText(context, context.getString(R.string.toast_undone), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun TaskRow(emoji: String, title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(emoji, fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(onClick = onAction, label = { Text(action) })
    }
}

/** 喂食打卡：选全家或指定猫 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedDailyDialog(
    cats: List<CatEntity>,
    onDismiss: () -> Unit,
    onSave: (catId: Long?) -> Unit,
) {
    var selectedCatId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.feed_who),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCatId == null,
                        onClick = { selectedCatId = null },
                        label = { Text(stringResource(R.string.common_family)) },
                    )
                    cats.forEach { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text("${cat.avatarEmoji} ${cat.name}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedCatId) }) { Text(stringResource(R.string.common_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** 记体重：选猫 + 千克输入 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeightRecordDialog(
    cats: List<CatEntity>,
    onDismiss: () -> Unit,
    onSave: (catId: Long, grams: Int) -> Unit,
) {
    if (cats.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.weight_title)) },
            text = { Text(stringResource(R.string.weight_need_cat)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) } },
        )
        return
    }

    var selected by remember { mutableStateOf(cats.first()) }
    var weightText by remember { mutableStateOf("") }
    val grams = weightText.toDoubleOrNull()?.times(1000)?.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { cat ->
                        FilterChip(
                            selected = selected.id == cat.id,
                            onClick = { selected = cat },
                            label = { Text("${cat.avatarEmoji} ${cat.name}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.weight_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = weightText.isNotBlank() && (grams == null || grams <= 0),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = grams != null && grams > 0,
                onClick = { onSave(selected.id, grams!!) },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** 撤销今日打卡：列出当天该类型的全部记录，逐条撤销 */
@Composable
private fun UndoDailyDialog(
    vm: AppViewModel,
    type: String,
    cats: List<CatEntity>,
    onDismiss: () -> Unit,
    onUndo: (DailyEntity) -> Unit,
) {
    var records by remember { mutableStateOf<List<DailyEntity>?>(null) }
    LaunchedEffect(type) { records = vm.todayDailiesFor(type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (type == DailyEntity.TYPE_FEED) R.string.undo_feed_title else R.string.undo_litter_title,
                ),
            )
        },
        text = {
            when {
                records == null -> Text(stringResource(R.string.common_loading))
                records!!.isEmpty() -> Text(stringResource(R.string.undo_empty))
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    records!!.forEach { d ->
                        val cat = cats.find { it.id == d.catId }
                        val name = cat?.let { "${it.avatarEmoji} ${it.name}" }
                            ?: stringResource(R.string.undo_family)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUndo(d) }) { Text(stringResource(R.string.common_undo)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

@Composable
private fun dueText(due: LocalDate): String {
    val today = LocalDate.now()
    val dateShort = stringResource(R.string.date_short, due.monthValue, due.dayOfMonth)
    return when {
        due.isBefore(today) -> stringResource(R.string.today_due_overdue, dateShort)
        due == today -> stringResource(R.string.today_due_today)
        else -> stringResource(
            R.string.today_due_on,
            dateShort,
            ChronoUnit.DAYS.between(today, due),
        )
    }
}

private fun toastComingSoon(context: android.content.Context) {
    Toast.makeText(context, context.getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
}
