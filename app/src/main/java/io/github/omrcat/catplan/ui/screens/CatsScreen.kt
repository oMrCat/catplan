package io.github.omrcat.catplan.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import io.github.omrcat.catplan.AppViewModel
import io.github.omrcat.catplan.domain.CatAge
import java.time.LocalDate

/**
 * 猫咪档案：真实数据库驱动的列表 + 添加猫咪表单。
 * 保存时会调用 CareScheduleGenerator 自动生成护理计划。
 */
@Composable
fun CatsScreen(vm: AppViewModel = viewModel(), onOpenCat: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val cats by vm.cats.collectAsStateWithLifecycle()
    val weights by vm.latestWeights.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("猫咪档案", style = MaterialTheme.typography.headlineSmall)

        if (cats.isEmpty()) {
            Text(
                "还没有猫咪，点下面的按钮添加吧 🐾",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        cats.forEach { cat ->
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCat(cat.id) },
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(cat.avatarEmoji, Modifier.padding(8.dp), fontSize = 20.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOfNotNull(cat.breed, CatAge.describe(LocalDate.ofEpochDay(cat.birthdayEpochDay)))
                                .joinToString(" · ")
                                .ifEmpty { "信息未填写" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            weights[cat.id]?.let { "%.1f kg".format(it / 1000.0) } ?: "暂无体重记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (cat.neutered) {
                        Text(
                            "已绝育",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("＋ 添加猫咪")
        }

        Text(
            "数据只保存在本机，不上传任何服务器。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddDialog) {
        AddCatDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, breed, gender, birthday, neutered, emoji ->
                showAddDialog = false
                vm.addCat(name, breed, gender, birthday, neutered, emoji) { count ->
                    Toast.makeText(context, "已为 $name 生成 $count 项护理计划 ✨", Toast.LENGTH_LONG).show()
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCatDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, breed: String, gender: String?, birthday: LocalDate, neutered: Boolean, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2024") }
    var month by remember { mutableStateOf("1") }
    var day by remember { mutableStateOf("1") }
    var isBoy by remember { mutableStateOf(true) }
    var neutered by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf("🍊") }

    val emojis = listOf("🍊", "🍡", "🐈‍⬛", "😹", "🐟", "🥷")
    val birthday = runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    val valid = name.isNotBlank() && birthday != null && birthday.isBefore(LocalDate.now())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加猫咪") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名字 *") },
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("出生年") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("月") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("日") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("品种（选填）") },
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = isBoy, onClick = { isBoy = true }, label = { Text("弟弟") })
                    FilterChip(selected = !isBoy, onClick = { isBoy = false }, label = { Text("妹妹") })
                    Checkbox(checked = neutered, onCheckedChange = { neutered = it })
                    Text("已绝育")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        name.trim(),
                        breed.trim(),
                        if (isBoy) "boy" else "girl",
                        birthday!!,
                        neutered,
                        emoji,
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
