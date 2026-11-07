package io.github.omrcat.catplan.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omrcat.catplan.AppViewModel
import io.github.omrcat.catplan.data.PhotoStore
import io.github.omrcat.catplan.R
import io.github.omrcat.catplan.data.local.CatEntity
import io.github.omrcat.catplan.data.local.HealthNoteEntity
import io.github.omrcat.catplan.data.local.PhotoEntity
import io.github.omrcat.catplan.domain.CatAge
import io.github.omrcat.catplan.domain.HealthFlags
import io.github.omrcat.catplan.domain.WeightChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * 猫咪详情：头部信息 + 体重记录 + 成长相册（v0.2）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatDetailScreen(
    catId: Long,
    onCatDeleted: () -> Unit = {},
    vm: AppViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val cats by vm.cats.collectAsStateWithLifecycle()
    val cat = cats.find { it.id == catId }
    val weights by remember(catId) { vm.observeWeights(catId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by remember(catId) { vm.observePhotos(catId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val healthNotes by remember(catId) { vm.observeHealthNotes(catId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var showWeightDialog by remember { mutableStateOf(false) }
    var showHealthDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteCatConfirm by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<PhotoEntity?>(null) }
    var healthNoteToDelete by remember { mutableStateOf<HealthNoteEntity?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            vm.addPhoto(context, catId, it) { ok ->
                if (!ok) Toast.makeText(context, "照片导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cat == null) {
            Text(
                "猫咪不存在或已被删除",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(cat.avatarEmoji, Modifier.padding(10.dp), fontSize = 28.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(
                        cat.breed,
                        CatAge.describe(LocalDate.ofEpochDay(cat.birthdayEpochDay)),
                    ).joinToString(" · ").ifEmpty { "信息未填写" },
                    style = MaterialTheme.typography.bodyMedium,
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.detail_edit))
            }
            OutlinedButton(
                onClick = { showDeleteCatConfirm = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.detail_delete_cat), color = MaterialTheme.colorScheme.error)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.weight_section), style = MaterialTheme.typography.titleMedium)
                val latest = weights.maxByOrNull { it.dateEpochDay }
                Text(
                    latest?.let {
                        "%.1f kg".format(it.weightGrams / 1000.0) +
                            " · " + LocalDate.ofEpochDay(it.dateEpochDay).let { d -> "${d.monthValue}月${d.dayOfMonth}日" }
                    } ?: "还没有记录",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (weights.size >= 2) {
                    val minGrams = weights.minOf { it.weightGrams }
                    val maxGrams = weights.maxOf { it.weightGrams }
                    Text(
                        "最高 %.2f kg · 最低 %.2f kg".format(maxGrams / 1000.0, minGrams / 1000.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val lineColor = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                        val data = WeightChart.mapPoints(
                            weights.map { it.dateEpochDay to it.weightGrams },
                            size.width,
                            size.height,
                        )
                        if (data.points.size >= 2) {
                            val path = Path().apply {
                                data.points.forEachIndexed { index, p ->
                                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                }
                            }
                            drawPath(
                                path,
                                color = lineColor,
                                style = Stroke(width = 6f, cap = StrokeCap.Round),
                            )
                        }
                        data.points.forEach { p ->
                            drawCircle(
                                color = lineColor,
                                radius = 8f,
                                center = Offset(p.x, p.y),
                            )
                        }
                    }
                }
                weights.take(5).forEach { w ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val d = LocalDate.ofEpochDay(w.dateEpochDay)
                        Text(
                            "${d.year}年${d.monthValue}月${d.dayOfMonth}日",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "%.1f kg".format(w.weightGrams / 1000.0),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showWeightDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.weight_add)) }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.album_section), style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) { Text(stringResource(R.string.album_add)) }
                }
                if (photos.isEmpty()) {
                    Text(
                        "拍下 TA 的每个瞬间吧 📷",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        photos.forEach { photo -> PhotoThumb(photo) { photoToDelete = photo } }
                    }
                }
                Text(
                    "照片保存在应用私有目录，点击照片可删除；不参与备份导出。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.health_section), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showHealthDialog = true }) { Text(stringResource(R.string.health_add)) }
                }
                if (healthNotes.isEmpty()) {
                    Text(
                        "记录食欲、精神与排泄情况，异常会醒目标出 🩺",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    healthNotes.take(5).forEach { note ->
                        val flags = HealthFlags.abnormalFlags(note.appetite, note.energy, note.poop, note.urine)
                        val d = LocalDate.ofEpochDay(note.dateEpochDay)
                        Row(
                            Modifier.fillMaxWidth().clickable { healthNoteToDelete = note },
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${d.year}年${d.monthValue}月${d.dayOfMonth}日",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                val observation = listOfNotNull(
                                    note.appetite?.let { "食欲 $it" },
                                    note.energy?.let { "精神 $it" },
                                    note.poop?.let { "便便 $it" },
                                    note.urine?.let { "尿液 $it" },
                                    note.text,
                                ).joinToString(" · ")
                                if (observation.isNotEmpty()) {
                                    Text(
                                        observation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (flags.isNotEmpty()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (flags.isNotEmpty()) {
                                Text(
                                    if (HealthFlags.urgent(flags)) "⚠️ 尽快就医" else "⚠️ 留意",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (healthNotes.size > 5) {
                        Text(
                            "更早的 ${healthNotes.size - 5} 条记录向下翻找 📜",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showWeightDialog && cat != null) {
        FixedCatWeightDialog(
            cat = cat,
            onDismiss = { showWeightDialog = false },
            onSave = { grams ->
                showWeightDialog = false
                vm.recordWeight(catId, grams)
                Toast.makeText(context, context.getString(R.string.toast_weight_saved), Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showHealthDialog && cat != null) {
        HealthNoteDialog(
            cat = cat,
            onDismiss = { showHealthDialog = false },
            onSave = { appetite, energy, poop, urine, text ->
                showHealthDialog = false
                vm.addHealthNote(catId, appetite, energy, poop, urine, text)
                Toast.makeText(context, context.getString(R.string.toast_health_saved), Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showEditDialog && cat != null) {
        CatFormDialog(
            cat = cat,
            onDismiss = { showEditDialog = false },
            onSave = { name, breed, gender, birthday, neutered, emoji, birthdayChanged ->
                showEditDialog = false
                vm.updateCat(
                    cat.copy(
                        name = name,
                        breed = breed.takeIf { it.isNotBlank() },
                        gender = gender,
                        birthdayEpochDay = birthday.toEpochDay(),
                        neutered = neutered,
                        avatarEmoji = emoji,
                    ),
                    birthdayChanged,
                ) {
                    if (birthdayChanged) {
                        Toast.makeText(context, context.getString(R.string.toast_plan_regen), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_cat_updated), Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    if (showDeleteCatConfirm && cat != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCatConfirm = false },
            title = { Text("删除 ${cat.name}？") },
            text = { Text(stringResource(R.string.detail_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCatConfirm = false
                        vm.deleteCat(cat) { onCatDeleted() }
                    },
                ) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteCatConfirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text(stringResource(R.string.album_delete_title)) },
            text = { Text(stringResource(R.string.album_delete_text)) },
            confirmButton = {
                TextButton(onClick = { vm.deletePhoto(photo); photoToDelete = null }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    healthNoteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { healthNoteToDelete = null },
            title = { Text(stringResource(R.string.health_delete_title)) },
            text = { Text(stringResource(R.string.album_delete_text)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteHealthNote(note); healthNoteToDelete = null }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { healthNoteToDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun PhotoThumb(photo: PhotoEntity, onClick: () -> Unit) {
    val bitmapState = remember(photo.filePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photo.filePath) {
        bitmapState.value = withContext(Dispatchers.IO) { PhotoStore.load(photo.filePath, 384) }
    }
    val bitmap = bitmapState.value
    Box(
        Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "成长照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FixedCatWeightDialog(cat: CatEntity, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val grams = text.toDoubleOrNull()?.times(1000)?.toInt()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_for, cat.name)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.weight_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = text.isNotBlank() && (grams == null || grams <= 0),
            )
        },
        confirmButton = {
            TextButton(enabled = grams != null && grams > 0, onClick = { onSave(grams!!) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthNoteDialog(
    cat: CatEntity,
    onDismiss: () -> Unit,
    onSave: (appetite: String?, energy: String?, poop: String?, urine: String?, text: String?) -> Unit,
) {
    var appetite by remember { mutableStateOf<String?>(null) }
    var energy by remember { mutableStateOf<String?>(null) }
    var poop by remember { mutableStateOf<String?>(null) }
    var urine by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.health_dialog_title, cat.name)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChipGroup("食欲", HealthNoteEntity.APPETITE_OPTIONS, appetite) { appetite = it }
                ChipGroup("精神", HealthNoteEntity.ENERGY_OPTIONS, energy) { energy = it }
                ChipGroup("便便", HealthNoteEntity.POOP_OPTIONS, poop) { poop = it }
                ChipGroup("尿液", HealthNoteEntity.URINE_OPTIONS, urine) { urine = it }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("备注（选填）") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(appetite, energy, poop, urine, text.trim().takeIf { it.isNotEmpty() }) },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(if (selected == option) null else option) },
                    label = { Text(option) },
                )
            }
        }
    }
}
