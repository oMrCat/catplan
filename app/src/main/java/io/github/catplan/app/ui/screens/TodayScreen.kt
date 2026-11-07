package io.github.catplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 今日页：问候 + 待办卡片 + 快捷记录入口。
 *
 * TODO(v0.1)：接入 CareTaskDao.observeDueWithin(...)，用真实数据驱动待办列表，
 * 并让「记录」按钮完成任务（标记 done 后按周期滚动到下一轮）。
 */
@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("早上好，铲屎官 🌤️", style = MaterialTheme.typography.headlineSmall)
        Text(
            "今天有 1 件事要办",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("今日待办", style = MaterialTheme.typography.titleMedium)
                TaskRow(icon = "💉", title = "橘子 · 体外驱虫", subtitle = "今天到期 · 每 1 个月一次", action = "记录")
                TaskRow(icon = "💊", title = "年糕 · 猫三联加强", subtitle = "11月13日 · 还有 7 天", action = "预约")
            }
        }

        Text("快捷记录", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { /* TODO(v0.1)：记体重 */ }, label = { Text("记体重") })
            AssistChip(onClick = { /* TODO(v0.1)：记喂食 */ }, label = { Text("记喂食") })
            AssistChip(onClick = { /* TODO(v0.1)：记用药 */ }, label = { Text("记用药") })
            AssistChip(onClick = { /* TODO(v0.1)：拍日记 */ }, label = { Text("拍日记") })
        }
    }
}

@Composable
private fun TaskRow(icon: String, title: String, subtitle: String, action: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(onClick = { /* TODO(v0.1)：完成/预约动作 */ }, label = { Text(action) })
    }
}
