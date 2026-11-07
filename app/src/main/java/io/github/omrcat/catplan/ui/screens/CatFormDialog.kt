package io.github.omrcat.catplan.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.omrcat.catplan.R
import io.github.omrcat.catplan.data.local.CatEntity
import java.time.LocalDate

/**
 * 添加/编辑猫咪双模式表单。
 * 编辑时若生日变更，onSave 携带 birthdayChanged=true，由调用方决定是否重建护理计划。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatFormDialog(
    cat: CatEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, breed: String, gender: String?, birthday: LocalDate, neutered: Boolean, emoji: String, birthdayChanged: Boolean) -> Unit,
) {
    val editing = cat != null
    var name by remember { mutableStateOf(cat?.name ?: "") }
    var breed by remember { mutableStateOf(cat?.breed ?: "") }
    var year by remember {
        mutableStateOf(cat?.birthdayEpochDay?.let { LocalDate.ofEpochDay(it).year.toString() } ?: "2024")
    }
    var month by remember {
        mutableStateOf(cat?.birthdayEpochDay?.let { LocalDate.ofEpochDay(it).monthValue.toString() } ?: "1")
    }
    var day by remember {
        mutableStateOf(cat?.birthdayEpochDay?.let { LocalDate.ofEpochDay(it).dayOfMonth.toString() } ?: "1")
    }
    var isBoy by remember { mutableStateOf(cat?.gender != "girl") }
    var neutered by remember { mutableStateOf(cat?.neutered ?: false) }
    var emoji by remember { mutableStateOf(cat?.avatarEmoji ?: "🍊") }

    val emojis = listOf("🍊", "🍡", "🐈‍⬛", "😹", "🐟", "🥷")
    val birthday = runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    val valid = name.isNotBlank() && birthday != null && birthday.isBefore(LocalDate.now())
    val originalBirthday = cat?.birthdayEpochDay?.let { LocalDate.ofEpochDay(it) }
    val birthdayChanged = editing && birthday != originalBirthday

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editing) R.string.form_edit_title else R.string.form_add_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.form_name)) },
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
                        label = { Text(stringResource(R.string.form_year)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text(stringResource(R.string.form_month)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text(stringResource(R.string.form_day)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (editing && birthdayChanged) {
                    Text(
                        "生日已修改：护理计划将按新生日重新生成（已完成记录保留）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text(stringResource(R.string.form_breed)) },
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = isBoy, onClick = { isBoy = true }, label = { Text(stringResource(R.string.form_boy)) })
                    FilterChip(selected = !isBoy, onClick = { isBoy = false }, label = { Text(stringResource(R.string.form_girl)) })
                    Checkbox(checked = neutered, onCheckedChange = { neutered = it })
                    Text(stringResource(R.string.form_neutered))
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
                        birthdayChanged,
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
