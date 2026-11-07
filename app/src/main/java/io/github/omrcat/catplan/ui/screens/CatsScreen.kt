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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omrcat.catplan.R
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
        Text(stringResource(R.string.cats_title), style = MaterialTheme.typography.headlineSmall)

        if (cats.isEmpty()) {
            Text(
                stringResource(R.string.cats_empty),
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
                                .ifEmpty { stringResource(R.string.cats_info_empty) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            weights[cat.id]?.let { "%.1f kg".format(it / 1000.0) }
                                ?: stringResource(R.string.cats_no_weight),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (cat.neutered) {
                        Text(
                            stringResource(R.string.form_neutered),
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
            Text(stringResource(R.string.cats_add))
        }

        Text(
            stringResource(R.string.cats_local_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddDialog) {
        CatFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, breed, gender, birthday, neutered, emoji, _ ->
                showAddDialog = false
                vm.addCat(name, breed, gender, birthday, neutered, emoji) { count ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_cat_added, name, count),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }
}

