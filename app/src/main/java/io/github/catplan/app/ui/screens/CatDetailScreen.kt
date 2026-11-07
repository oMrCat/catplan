package io.github.catplan.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.catplan.app.AppViewModel
import io.github.catplan.app.data.PhotoStore
import io.github.catplan.app.data.local.CatEntity
import io.github.catplan.app.data.local.PhotoEntity
import io.github.catplan.app.domain.CatAge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * 猫咪详情：头部信息 + 体重记录 + 成长相册（v0.2）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatDetailScreen(catId: Long, vm: AppViewModel = viewModel(), modifier: Modifier = Modifier) {
    val cats by vm.cats.collectAsStateWithLifecycle()
    val cat = cats.find { it.id == catId }
    val weights by remember(catId) { vm.observeWeights(catId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by remember(catId) { vm.observePhotos(catId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var showWeightDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<PhotoEntity?>(null) }

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

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("体重记录", style = MaterialTheme.typography.titleMedium)
                val latest = weights.maxByOrNull { it.dateEpochDay }
                Text(
                    latest?.let {
                        "%.1f kg".format(it.weightGrams / 1000.0) +
                            " · " + LocalDate.ofEpochDay(it.dateEpochDay).let { d -> "${d.monthValue}月${d.dayOfMonth}日" }
                    } ?: "还没有记录",
                    style = MaterialTheme.typography.titleLarge,
                )
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
                ) { Text("＋ 记体重") }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("成长相册", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) { Text("＋ 添加照片") }
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
    }

    if (showWeightDialog && cat != null) {
        FixedCatWeightDialog(
            cat = cat,
            onDismiss = { showWeightDialog = false },
            onSave = { grams ->
                showWeightDialog = false
                vm.recordWeight(catId, grams)
                Toast.makeText(context, "体重已记录 ✅", Toast.LENGTH_SHORT).show()
            },
        )
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("删除这张照片？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deletePhoto(photo); photoToDelete = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun PhotoThumb(photo: PhotoEntity, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, photo.filePath) {
        value = withContext(Dispatchers.IO) { PhotoStore.load(photo.filePath, 384) }
    }
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
        title = { Text("给 ${cat.name} 记体重") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("体重 (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = text.isNotBlank() && (grams == null || grams <= 0),
            )
        },
        confirmButton = {
            TextButton(enabled = grams != null && grams > 0, onClick = { onSave(grams!!) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
