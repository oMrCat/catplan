package io.github.catplan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 猫咪档案列表。
 *
 * TODO(v0.1)：
 *  1. CatDao.observeAll() 驱动列表，替换静态示例
 *  2. 「添加猫咪」表单：名字 / 生日 / 品种 / 是否绝育，保存后调用
 *     CareScheduleGenerator.generate(...) 生成护理计划写入 care_tasks
 *  3. 点击卡片进详情：体重曲线（WeightDao）+ 健康时间轴
 */
@Composable
fun CatsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("猫咪档案", style = MaterialTheme.typography.headlineSmall)

        ElevatedCard(Modifier.fillMaxWidth()) {
            CatRow(emoji = "🍊", name = "橘子", info = "中华田园橘猫 · 2 岁 3 个月", extra = "4.2 kg · 已绝育")
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            CatRow(emoji = "🍡", name = "年糕", info = "英短 · 1 岁", extra = "3.6 kg")
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            CatRow(emoji = "🐈‍⬛", name = "煤球", info = "孟买猫 · 4 岁", extra = "5.3 kg")
        }

        OutlinedButton(
            onClick = { /* TODO(v0.1)：打开添加猫咪表单 */ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("＋ 添加猫咪")
        }

        Text(
            "以上为预览示例数据，正式版将来自本地数据库。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatRow(emoji: String, name: String, info: String, extra: String) {
    Row(
        Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(emoji, Modifier.padding(8.dp), fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                extra,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
