package com.mochimoney.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.ui.CategoryUi
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.components.CategoryIconBubble
import com.mochimoney.app.ui.components.DonutSegment
import com.mochimoney.app.ui.components.KawaiiDonutChart
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiMascot
import com.mochimoney.app.ui.components.MochiMood
import com.mochimoney.app.ui.components.SectionTitle
import com.mochimoney.app.ui.components.categoryColor
import com.mochimoney.app.ui.components.formatCurrency
import com.mochimoney.app.ui.components.iconForCategory
import com.mochimoney.app.ui.components.rememberMochiHapticClick

@Composable
fun DashboardScreen(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val wide = maxWidth >= 720.dp
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (wide) 24.dp else 14.dp,
                vertical = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SpendingPieCard(state = state, onMochiTap = actions.onRefresh) }
            item { AnalyticsBlock(state = state) }
            item { state.scanStatus?.let { ScanStatusCard(it) } }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SpendingPieCard(
    state: MochiMoneyUiState,
    onMochiTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slices = state.categories
        .filterNot { it.id == DefaultCategoryIds.UNCATEGORIZED || it.id == DefaultCategoryIds.INCOME }
        .filter { it.spentPaise > 0L }
        .sortedByDescending { it.spentPaise }
    val total = slices.sumOf { it.spentPaise }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MochiCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle(
                title = "This month",
                trailing = {
                    Text(
                        text = if (state.isRefreshing) "scanning…" else "tap mochi to refresh!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryPie(
                    categories = slices,
                    total = total,
                    onMochiTap = onMochiTap,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.size(150.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (total == 0L) {
                        Text(
                            "Tap mochi to scan SMS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            formatCurrency(total),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "spent · ${slices.size} categories",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        slices.take(3).forEach { category ->
                            CompactLegendRow(category = category, total = total)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPie(
    categories: List<CategoryUi>,
    total: Long,
    onMochiTap: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressTarget = if (pressed) 0.88f else 1f
    val pressScale by animateFloatAsState(
        targetValue = pressTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "mochi tap bounce",
    )
    // Gentle continuous pulse while refreshing so the user sees a clear "working" state
    // even when the scan finishes in a few hundred ms.
    val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pulse.snapTo(1f)
            while (isRefreshing) {
                pulse.animateTo(
                    targetValue = 0.92f,
                    animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                )
                pulse.animateTo(
                    targetValue = 1.04f,
                    animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                )
            }
        }
        pulse.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    val scale = pressScale * pulse.value
    val click = rememberMochiHapticClick(onClick = onMochiTap)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (total > 0L) {
            KawaiiDonutChart(
                segments = categories.map { category ->
                    DonutSegment(
                        fraction = category.spentPaise.toFloat() / total.toFloat(),
                        color = categoryColor(category.kind),
                    )
                },
                modifier = Modifier.matchParentSize(),
                strokeWidth = 20.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            )
        }
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = click,
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            MochiMascot(
                modifier = Modifier.size(74.dp),
                size = 74.dp,
                mood = if (isRefreshing) MochiMood.Focused else MochiMood.Happy,
            )
        }
    }
}

@Composable
private fun CompactLegendRow(category: CategoryUi, total: Long) {
    val percent = ((category.spentPaise * 100f) / total).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(categoryColor(category.kind)),
        )
        Text(
            category.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnalyticsBlock(state: MochiMoneyUiState, modifier: Modifier = Modifier) {
    val spent = state.totalSpentThisMonth
    val incoming = state.incomingThisMonth
    val budget = state.monthlyBudget
    val left = (budget - spent).coerceAtLeast(0)
    val pace = if (budget > 0) (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f) else 0f
    val topCategory = state.categories
        .filterNot { it.id == DefaultCategoryIds.UNCATEGORIZED || it.id == DefaultCategoryIds.INCOME }
        .maxByOrNull { it.spentPaise }
    val txnCount = state.transactions.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MochiCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Analytics")
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 520.dp) 4 else 2
                AnalyticsGrid(
                    items = listOf(
                        AnalyticItem("Spent", formatCurrency(spent)),
                        AnalyticItem("Left", formatCurrency(left)),
                        AnalyticItem("Received", formatCurrency(incoming)),
                        AnalyticItem("Transactions", txnCount.toString()),
                    ),
                    columns = cols,
                )
            }
            if (budget > 0) {
                BudgetPaceRow(pace = pace, spent = spent, budget = budget)
            }
            if (topCategory != null && topCategory.spentPaise > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryIconBubble(
                        icon = iconForCategory(topCategory.kind),
                        color = categoryColor(topCategory.kind),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Top spend · ${topCategory.label}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${formatCurrency(topCategory.spentPaise)} so far",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.uncategorizedCount > 0) {
                Text(
                    "${state.uncategorizedCount} transactions need a category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class AnalyticItem(val label: String, val value: String)

@Composable
private fun AnalyticsGrid(items: List<AnalyticItem>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { item ->
                    AnalyticChip(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AnalyticChip(item: AnalyticItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MochiCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            item.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(item.value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BudgetPaceRow(pace: Float, spent: Long, budget: Long) {
    val animatedPace by animateFloatAsState(
        targetValue = pace,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "budget pace",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Budget pace",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${formatCurrency(spent)} / ${formatCurrency(budget)}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPace)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ScanStatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MochiCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
