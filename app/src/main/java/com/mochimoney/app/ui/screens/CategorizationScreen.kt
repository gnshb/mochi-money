package com.mochimoney.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.ui.CategoryUi
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.UpiTransactionUi
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiMascot
import com.mochimoney.app.ui.components.MochiMood
import com.mochimoney.app.ui.components.SectionTitle
import com.mochimoney.app.ui.components.categoryColor
import com.mochimoney.app.ui.components.formatCurrency
import com.mochimoney.app.ui.components.formatTxnDate
import com.mochimoney.app.ui.components.iconForCategory
import com.mochimoney.app.ui.components.rememberMochiHapticClick

private const val KeywordHelpHeadline = "How matching works"
private const val KeywordHelpBody =
    "One pattern per line. First line that matches the merchant name wins. " +
        "Case-insensitive. Plain text or regex."

@Composable
fun CategorizationScreen(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    val uncategorized = state.transactions.filter {
        it.categoryId == null || it.categoryId == DefaultCategoryIds.UNCATEGORIZED
    }
    val pickableCategories = state.categories.filterNot {
        it.id == DefaultCategoryIds.UNCATEGORIZED
    }

    com.mochimoney.app.ui.components.DismissFocusBox(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uncategorized.isEmpty()) {
                item { AllTidyCard() }
            } else {
                items(uncategorized, key = { it.id }) { transaction ->
                    UncategorizedCard(
                        transaction = transaction,
                        categories = pickableCategories,
                        onPick = { categoryId ->
                            actions.onCategorizeTransaction(transaction.id, categoryId)
                        },
                    )
                }
            }
            categoryRulesItems(this, state = state, actions = actions)
            item { Spacer(Modifier.height(20.dp)) }
        }
        }
    }
}

@Composable
private fun UncategorizedCard(
    transaction: UpiTransactionUi,
    categories: List<CategoryUi>,
    onPick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatTxnDate(transaction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatCurrency(transaction.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    val onClick = rememberMochiHapticClick { onPick(category.id) }
                    FilterChip(
                        selected = false,
                        onClick = onClick,
                        label = { Text(category.label) },
                        leadingIcon = {
                            Icon(
                                iconForCategory(category.kind),
                                contentDescription = null,
                                tint = categoryColor(category.kind),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = categoryColor(category.kind).copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllTidyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MochiMascot(size = 140.dp, mood = MochiMood.Happy)
            Text(
                "Everything is tidy",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "All detected payments have categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun categoryRulesItems(
    scope: LazyListScope,
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    val editableCategories = state.categories
        .filterNot { it.id == DefaultCategoryIds.UNCATEGORIZED }
        .sortedBy { if (it.id == DefaultCategoryIds.OTHER) 1 else 0 }
    scope.item(key = "rules-section") {
        SectionTitle("Categories and keywords")
    }
    scope.item(key = "rules-new") { NewCategoryCard(actions = actions) }
    scope.items(editableCategories, key = { "kw-${it.id}" }) { category ->
        CategoryKeywordCard(category = category, actions = actions)
    }
    scope.item(key = "rules-help") { KeywordHelpCard() }
}

@Composable
private fun KeywordHelpCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(KeywordHelpHeadline, style = MaterialTheme.typography.titleMedium)
            Text(
                KeywordHelpBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewCategoryCard(actions: MochiMoneyActions) {
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var newCategoryKeywords by rememberSaveable { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("New category", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Name") },
            )
            OutlinedTextField(
                value = newCategoryKeywords,
                onValueChange = { newCategoryKeywords = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Patterns (one per line)") },
                placeholder = {
                    Text(
                        "swiggy|zomato|dominos\n^uber|^ola\nbigbasket|dmart|zepto\napollo|pharmeasy|medplus",
                    )
                },
            )
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            val addCategory = rememberMochiHapticClick(enabled = newCategoryName.isNotBlank()) {
                focusManager.clearFocus()
                actions.onUpsertCategory(newCategoryName, newCategoryKeywords, null)
                newCategoryName = ""
                newCategoryKeywords = ""
            }
            Button(onClick = addCategory, enabled = newCategoryName.isNotBlank()) {
                Text("Add category")
            }
        }
    }
}

@Composable
private fun CategoryKeywordCard(category: CategoryUi, actions: MochiMoneyActions) {
    var keywords by rememberSaveable(category.id, category.keywordsCsv) {
        mutableStateOf(category.keywordsCsv)
    }
    val dirty = keywords != category.keywordsCsv

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    iconForCategory(category.kind),
                    contentDescription = null,
                    tint = categoryColor(category.kind),
                )
                Text(category.label, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Patterns (one per line)") },
            )
            AnimatedVisibility(visible = dirty) {
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                OutlinedButton(
                    onClick = rememberMochiHapticClick {
                        focusManager.clearFocus()
                        actions.onUpsertCategory(category.label, keywords, category.id)
                    },
                ) {
                    Text("Save ${category.label}")
                }
            }
        }
    }
}
