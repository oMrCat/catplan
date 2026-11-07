package io.github.catplan.app.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.catplan.app.AppViewModel
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.DailyEntity
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

    val fedToday = dailies.any { it.type == DailyEntity.TYPE_FEED }
    val litterToday = dailies.any { it.type == DailyEntity.TYPE_LITTER }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("早上好，铲屎官 🌤️", style = MaterialTheme.typography.headlineSmall)
        Text(
            "连续记录 $streak 天 · " + if (tasks.isEmpty()) "今天没有待办，安心吸猫" else "有 ${tasks.size} 件护理待办",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("今日待办", style = MaterialTheme.typography.titleMedium)
                if (tasks.isEmpty()) {
                    Text(
                        "近 7 天没有护理安排。去「档案」添加猫咪，护理计划会自动生成 ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tasks.forEach { item ->
                        TaskRow(
                            emoji = item.emoji,
                            title = "${item.catName} · ${item.task.title}",
                            subtitle = dueText(item.dueDate),
                            action = "完成",
                            onAction = { vm.completeTask(item) },
                        )
                    }
                }
            }
        }

        Text("日常打卡", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { showWeightDialog = true }, label = { Text("记体重") })
            AssistChip(
                onClick = { showFeedDialog = true },
                label = { Text(if (fedToday) "已喂 ✓" else "记喂食") },
            )
            AssistChip(
                onClick = {
                    if (!litterToday) {
                        vm.addDaily(DailyEntity.TYPE_LITTER, catId = null)
                        Toast.makeText(context, "铲屎打卡完成 🧹", Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text(if (litterToday) "已铲 ✓" else "铲屎") },
            )
            AssistChip(onClick = { toastComingSoon(context) }, label = { Text("拍日记") })
        }
    }

    if (showWeightDialog) {
        WeightRecordDialog(
            cats = cats,
            onDismiss = { showWeightDialog = false },
            onSave = { catId, grams ->
                showWeightDialog = false
                vm.recordWeight(catId, grams)
                Toast.makeText(context, "体重已记录 ✅", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "喂食打卡完成 🍚", Toast.LENGTH_SHORT).show()
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
        title = { Text("记喂食") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "这顿是给谁喂的？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCatId == null,
                        onClick = { selectedCatId = null },
                        label = { Text("全家") },
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
            TextButton(onClick = { onSave(selectedCatId) }) { Text("打卡") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
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
            title = { Text("记体重") },
            text = { Text("先去「档案」添加一只猫咪吧 🐾") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("好的") } },
        )
        return
    }

    var selected by remember { mutableStateOf(cats.first()) }
    var weightText by remember { mutableStateOf("") }
    val grams = weightText.toDoubleOrNull()?.times(1000)?.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记体重") },
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
                    label = { Text("体重 (kg)") },
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
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun dueText(due: LocalDate): String {
    val today = LocalDate.now()
    return when {
        due.isBefore(today) -> "${due.monthValue}月${due.dayOfMonth}日 · 已逾期"
        due == today -> "今天到期"
        else -> "${due.monthValue}月${due.dayOfMonth}日 · 还有 ${ChronoUnit.DAYS.between(today, due)} 天"
    }
}

private fun toastComingSoon(context: android.content.Context) {
    Toast.makeText(context, "这个功能 v0.3 就来 🚧", Toast.LENGTH_SHORT).show()
}
