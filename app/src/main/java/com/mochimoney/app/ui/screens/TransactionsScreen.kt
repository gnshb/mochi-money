package com.mochimoney.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.mochimoney.app.ui.CategoryUi
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.UpiTransactionUi
import com.mochimoney.app.ui.components.CategoryIconBubble
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiIcons
import com.mochimoney.app.ui.components.SectionTitle
import com.mochimoney.app.ui.components.TransactionRow
import com.mochimoney.app.ui.components.categoryColor
import com.mochimoney.app.ui.components.formatCurrency
import com.mochimoney.app.ui.components.iconForCategory

@Composable
fun TransactionsScreen(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailsForId by remember { mutableStateOf<String?>(null) }
    var renameForId by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val openDetails: (String) -> Unit = { detailsForId = it }
    val openRename: (String) -> Unit = { renameForId = it }

    val today = java.time.LocalDate.now()
    val filteredTransactions = state.transactions.filter { transaction ->
        val thisMonth = transaction.occurredOn.year == today.year && transaction.occurredOn.month == today.month
        val matchesQuery = query.isBlank() ||
            transaction.title.contains(query, ignoreCase = true) ||
            transaction.subtitle.contains(query, ignoreCase = true)
        val matchesCategory = selectedCategoryId == null || transaction.categoryId == selectedCategoryId
        thisMonth && matchesQuery && matchesCategory
    }

    val detailsTxn = detailsForId?.let { id -> state.transactions.firstOrNull { it.id == id } }
    if (detailsTxn != null) {
        TransactionDetailsDialog(
            transaction = detailsTxn,
            onDismiss = { detailsForId = null },
        )
    }

    val editTxn = renameForId?.let { id -> state.transactions.firstOrNull { it.id == id } }
    if (editTxn != null) {
        EditTransactionDialog(
            transaction = editTxn,
            onSave = { name, amountPaise ->
                actions.onEditTransaction(editTxn.id, name, amountPaise)
                renameForId = null
            },
            onDelete = {
                actions.onDeleteTransaction(editTxn.id)
                renameForId = null
            },
            onDismiss = { renameForId = null },
        )
    }

    if (showAdd) {
        AddTransactionDialog(
            categories = state.categories,
            onAdd = { title, amountPaise, occurredOn, isIncoming, categoryId, note ->
                actions.onAddManualTransaction(title, amountPaise, occurredOn, isIncoming, categoryId, note)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    com.mochimoney.app.ui.components.DismissFocusBox(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 860.dp
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TransactionsList(
                    state = state,
                    query = query,
                    onQueryChange = { query = it },
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it },
                    filteredTransactions = filteredTransactions,
                    actions = actions,
                    onOpenDetails = openDetails,
                    onOpenRename = openRename,
                    onAddClick = { showAdd = true },
                    modifier = Modifier.weight(1.45f),
                )
                TransactionSummaryPane(
                    transactions = filteredTransactions,
                    categories = state.categories,
                    modifier = Modifier.weight(0.85f),
                )
            }
        } else {
            TransactionsList(
                state = state,
                query = query,
                onQueryChange = { query = it },
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                filteredTransactions = filteredTransactions,
                actions = actions,
                onOpenDetails = openDetails,
                onOpenRename = openRename,
                onAddClick = { showAdd = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    }
}

@Composable
private fun TransactionsList(
    state: MochiMoneyUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    filteredTransactions: List<UpiTransactionUi>,
    actions: MochiMoneyActions,
    onOpenDetails: (String) -> Unit,
    onOpenRename: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Search payments") },
                    leadingIcon = { Icon(MochiIcons.Transactions, contentDescription = null) },
                )
                FilledIconButton(onClick = onAddClick) {
                    Icon(MochiIcons.Add, contentDescription = "Add transaction")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        ),
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    val tint = com.mochimoney.app.ui.components.categoryColor(category.kind)
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.label) },
                        leadingIcon = {
                            Icon(
                                iconForCategory(category.kind),
                                contentDescription = null,
                                tint = tint,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = tint.copy(alpha = 0.14f),
                            selectedContainerColor = tint.copy(alpha = 0.32f),
                        ),
                    )
                }
            }
        }
        if (filteredTransactions.isEmpty()) {
            item {
                EmptyTransactionsCard()
            }
        } else {
            items(filteredTransactions, key = { it.id }) { transaction ->
                val category = state.categories.firstOrNull { it.id == transaction.categoryId }
                TransactionRow(
                    transaction = transaction,
                    category = category,
                    onClick = { onOpenDetails(transaction.id) },
                    aiEnabled = state.settings.llm.enabled,
                    onAiClick = { actions.onInferCounterparty(transaction.id) },
                    onEditClick = { onOpenRename(transaction.id) },
                )
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TransactionSummaryPane(
    transactions: List<UpiTransactionUi>,
    categories: List<CategoryUi>,
    modifier: Modifier = Modifier,
) {
    val outgoing = transactions.filterNot { it.isIncoming }.sumOf { it.amountPaise }
    val incoming = transactions.filter { it.isIncoming }.sumOf { it.amountPaise }
    val topCategory = categories.maxByOrNull { category ->
        transactions.filter { it.categoryId == category.id && !it.isIncoming }.sumOf { it.amountPaise }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Slice summary")
        SummaryCard("Outgoing", formatCurrency(outgoing))
        SummaryCard("Incoming", formatCurrency(incoming))
        if (topCategory != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MochiCardShape,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryIconBubble(
                        icon = iconForCategory(topCategory.kind),
                        color = categoryColor(topCategory.kind),
                    )
                    Column {
                        Text("Most active category", style = MaterialTheme.typography.labelLarge)
                        Text(topCategory.label, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun TransactionDetailsDialog(
    transaction: UpiTransactionUi,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    com.mochimoney.app.ui.components.formatTxnDate(transaction),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    com.mochimoney.app.ui.components.formatCurrency(transaction.amountPaise),
                    style = MaterialTheme.typography.headlineMedium,
                )
                val body = transaction.smsBody
                if (body.isNullOrBlank()) {
                    Text(
                        "No SMS body stored for this transaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        shape = MochiCardShape,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun EditTransactionDialog(
    transaction: UpiTransactionUi,
    onSave: (name: String, amountPaise: Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(transaction.id) { mutableStateOf(transaction.title) }
    var amountText by rememberSaveable(transaction.id) { mutableStateOf(rupeeText(transaction.amountPaise)) }

    val amountPaise = runCatching {
        java.math.BigDecimal(amountText)
            .movePointRight(2)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }.getOrNull()?.takeIf { it > 0L }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Amount in INR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "Renaming also updates matching payments and remembers it for future ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (transaction.isManual) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Delete transaction") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountPaise ?: transaction.amountPaise) },
                enabled = name.isNotBlank() && amountPaise != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MochiCardShape,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/** Renders paise as an editable rupee string, dropping the decimals when the amount is whole. */
private fun rupeeText(amountPaise: Long): String =
    if (amountPaise % 100L == 0L) {
        (amountPaise / 100L).toString()
    } else {
        java.math.BigDecimal(amountPaise).movePointLeft(2).toPlainString()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    categories: List<CategoryUi>,
    onAdd: (title: String, amountPaise: Long, occurredOn: LocalDate, isIncoming: Boolean, categoryId: String?, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isIncoming by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var occurredOnEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val occurredOn = LocalDate.ofEpochDay(occurredOnEpochDay)

    // Rupees → paise, rounding to the nearest paisa. null until a positive amount is entered.
    val amountPaise = runCatching {
        java.math.BigDecimal(amountText)
            .movePointRight(2)
            .setScale(0, java.math.RoundingMode.HALF_UP)
            .toLong()
    }.getOrNull()?.takeIf { it > 0L }
    val canSave = title.isNotBlank() && amountPaise != null

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = occurredOn.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        occurredOnEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncoming,
                        onClick = { isIncoming = false },
                        label = { Text("Outgoing") },
                    )
                    FilterChip(
                        selected = isIncoming,
                        onClick = { isIncoming = true },
                        label = { Text("Incoming") },
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isIncoming) "From" else "Paid to") },
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Amount in INR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Date: ${occurredOn.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                }
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Auto") },
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(title.trim(), amountPaise ?: 0L, occurredOn, isIncoming, selectedCategoryId, note.trim().ifBlank { null })
                },
                enabled = canSave,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MochiCardShape,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("No matches", style = MaterialTheme.typography.titleLarge)
            Text(
                "Try a different search or category filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
