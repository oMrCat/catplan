package io.github.omrcat.catplan.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
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
import io.github.omrcat.catplan.data.local.ExpenseEntity
import io.github.omrcat.catplan.domain.Expense
import io.github.omrcat.catplan.domain.LedgerMath
import java.time.LocalDate

/**
 * 账本（v0.2）：本月总支出、类目占比、按猫分摊、最近记录。
 * 统计逻辑在 LedgerMath（纯函数，有单元测试）。
 */
@Composable
fun LedgerScreen(vm: AppViewModel = viewModel(), modifier: Modifier = Modifier) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val cats by vm.cats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    val summary = LedgerMath.summarize(
        expenses.map {
            Expense(it.expense.amountCents, it.expense.category, it.expense.dateEpochDay) to it.catName
        },
        today.year,
        today.monthValue,
    )
    val maxCategory = summary.byCategory.maxOfOrNull { it.second } ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ledger_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            "${today.year}年${today.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.ledger_total), style = MaterialTheme.typography.titleMedium)
                Text(
                    LedgerMath.formatCents(summary.totalCents),
                    style = MaterialTheme.typography.headlineLarge,
                )
                if (summary.byCategory.isEmpty()) {
                    Text(
                        stringResource(R.string.ledger_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    summary.byCategory.forEach { (label, cents) ->
                        BarRow(
                            label = label,
                            amountText = LedgerMath.formatCents(cents),
                            fraction = LedgerMath.fraction(cents, maxCategory),
                        )
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ledger_by_cat), style = MaterialTheme.typography.titleMedium)
                if (summary.byCat.isEmpty()) {
                    Text(stringResource(R.string.ledger_dash), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    summary.byCat.forEach { (name, cents) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                LedgerMath.formatCents(cents),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.ledger_recent), style = MaterialTheme.typography.titleMedium)
                if (expenses.isEmpty()) {
                    Text(stringResource(R.string.ledger_no_records), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    expenses.take(8).forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(item.emoji, fontSize = 18.sp)
                                Column {
                                    Text(
                                        "${item.catName} · ${item.expense.category}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        listOfNotNull(
                                            item.expense.note,
                                            "${item.date.monthValue}月${item.date.dayOfMonth}日",
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                LedgerMath.formatCents(item.expense.amountCents),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ledger_add))
        }

        Text(
            stringResource(R.string.ledger_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddDialog) {
        AddExpenseDialog(
            cats = cats,
            onDismiss = { showAddDialog = false },
            onSave = { catId, cents, category, note ->
                showAddDialog = false
                vm.addExpense(catId, cents, category, note)
                Toast.makeText(context, context.getString(R.string.toast_expense_saved), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun BarRow(label: String, amountText: String, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                amountText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
    cats: List<CatEntity>,
    onDismiss: () -> Unit,
    onSave: (catId: Long?, cents: Long, category: String, note: String?) -> Unit,
) {
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf(ExpenseEntity.CATEGORY_FOOD) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cents = amountText.toDoubleOrNull()?.times(100)?.toLong()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.expense_who), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.expense_category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseEntity.CATEGORIES.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.expense_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && cents == null,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.expense_note)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cents != null,
                onClick = { onSave(selectedCatId, cents!!, category, note.trim().takeIf { it.isNotEmpty() }) },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
