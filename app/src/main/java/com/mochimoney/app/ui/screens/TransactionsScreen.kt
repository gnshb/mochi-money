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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    val renameTxn = renameForId?.let { id -> state.transactions.firstOrNull { it.id == id } }
    if (renameTxn != null) {
        RenameCounterpartyDialog(
            transaction = renameTxn,
            onSave = { name ->
                actions.onRenameCounterparty(renameTxn.id, name)
                renameForId = null
            },
            onDismiss = { renameForId = null },
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search payments") },
                leadingIcon = { Icon(MochiIcons.Transactions, contentDescription = null) },
            )
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
private fun RenameCounterpartyDialog(
    transaction: UpiTransactionUi,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(transaction.id) { mutableStateOf(transaction.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename counterparty", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                )
                Text(
                    "Also renames matching payments and remembers it for future ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
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
