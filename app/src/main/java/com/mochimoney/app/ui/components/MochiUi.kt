package com.mochimoney.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mochimoney.app.ui.CategoryKind
import com.mochimoney.app.ui.CategoryUi
import com.mochimoney.app.ui.UpiTransactionUi
import com.mochimoney.app.ui.theme.MochiCoral
import com.mochimoney.app.ui.theme.MochiLemon
import com.mochimoney.app.ui.theme.MochiLilac
import com.mochimoney.app.ui.theme.MochiMint
import com.mochimoney.app.ui.theme.MochiPositive
import com.mochimoney.app.ui.theme.MochiRose
import com.mochimoney.app.ui.theme.MochiSky
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.min

/** Wraps content so taps outside any focused TextField clear focus + hide the keyboard. */
@Composable
fun DismissFocusBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
    ) { content() }
}

private val CurrencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val DateOnlyFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DateTimeFormatterShort = DateTimeFormatter.ofPattern("d MMM, h:mm a")
private val TimeOnlyFormatter = DateTimeFormatter.ofPattern("h:mm a")

fun formatTxnDate(transaction: UpiTransactionUi): String {
    val time = transaction.occurredTime ?: return transaction.occurredOn.format(DateOnlyFormatter)
    return java.time.LocalDateTime.of(transaction.occurredOn, time).format(DateTimeFormatterShort)
}
val MochiCardShape = RoundedCornerShape(28.dp)

@Composable
fun Modifier.mochiPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "mochi press scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberMochiHapticClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(enabled, haptic, onClick) {
        {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (trailing != null) trailing()
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val pop = remember { Animatable(0.98f) }
    LaunchedEffect(value) {
        pop.snapTo(0.98f)
        pop.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .drawWithContent {
                drawRoundRect(
                    color = color.copy(alpha = 0.08f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                )
                drawContent()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBubble(icon = icon, color = color)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(value, style = MaterialTheme.typography.headlineMedium)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: UpiTransactionUi,
    category: CategoryUi?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickWithHaptics = rememberMochiHapticClick(onClick = onClick)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .mochiPressScale(interactionSource)
            .clip(MochiCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = clickWithHaptics,
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MochiCardShape,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val kind = category?.kind ?: CategoryKind.Other
            CategoryIconBubble(
                icon = iconForCategory(kind),
                color = categoryColor(kind),
            )
            Spacer(Modifier.width(12.dp))
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (category == null) {
                    Spacer(Modifier.height(6.dp))
                    AssistChip(onClick = onClick, label = { Text("Needs category") })
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = signedCurrency(transaction),
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.isIncoming) MochiPositive else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun CategoryProgressRow(
    category: CategoryUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = category.budgetPaise?.let { budget ->
        min(1f, category.spentPaise.toFloat() / budget.toFloat())
    } ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700),
        label = "category progress",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val clickWithHaptics = rememberMochiHapticClick(onClick = onClick)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .mochiPressScale(interactionSource)
            .clip(MochiCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = clickWithHaptics,
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = MochiCardShape,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBubble(icon = iconForCategory(category.kind), color = categoryColor(category.kind))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (category.budgetPaise == null) "Tracked without a budget" else "${formatCurrency(category.spentPaise)} of ${formatCurrency(category.budgetPaise)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatCurrency(category.spentPaise), style = MaterialTheme.typography.labelLarge)
            }
            if (category.budgetPaise != null) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = categoryColor(category.kind),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CategoryIconBubble(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val iconPop by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 260),
        label = "category icon pop",
    )
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .drawWithContent {
                drawCircle(
                    color = Color.White.copy(alpha = 0.62f),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width * 0.36f, size.height * 0.28f),
                )
                drawContent()
                drawCircle(
                    color = color.copy(alpha = 0.16f),
                    radius = size.minDimension * 0.11f,
                    center = Offset(size.width * 0.72f, size.height * 0.26f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.72f),
                    radius = size.minDimension * 0.045f,
                    center = Offset(size.width * 0.76f, size.height * 0.22f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconPop
                    scaleY = iconPop
                },
        )
    }
}

data class DonutSegment(
    val fraction: Float,
    val color: Color,
)

@Composable
fun KawaiiDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        var startAngle = -90f
        segments
            .filter { it.fraction > 0f }
            .forEach { segment ->
                val sweep = segment.fraction.coerceIn(0f, 1f) * 360f * reveal.value
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                startAngle += sweep
            }
        drawCircle(
            color = MochiLemon.copy(alpha = 0.45f * reveal.value),
            radius = stroke * 0.28f,
            center = Offset(size.width * 0.74f, size.height * 0.18f),
        )
    }
}

@Composable
private fun KawaiiSuccessAccent(
    modifier: Modifier = Modifier,
    color: Color,
) {
    val sparkle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sparkle.snapTo(0f)
        sparkle.animateTo(1f, animationSpec = tween(durationMillis = 460))
    }
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.12f * sparkle.value
        drawCircle(
            color = color.copy(alpha = 0.08f),
            radius = size.minDimension * 0.42f * sparkle.value,
            center = center,
        )
        drawLine(
            color = MochiLemon.copy(alpha = 0.58f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = MochiLemon.copy(alpha = 0.58f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

fun formatCurrency(amountPaise: Long): String {
    return CurrencyFormatter.format(amountPaise / 100.0)
}

fun categoryColor(kind: CategoryKind): Color = when (kind) {
    CategoryKind.Food -> MochiCoral
    CategoryKind.Groceries -> MochiMint
    CategoryKind.Travel -> MochiSky
    CategoryKind.Bills -> MochiRose
    CategoryKind.Shopping -> MochiLilac
    CategoryKind.Health -> Color(0xFFE1528C)
    CategoryKind.Salary -> MochiPositive
    CategoryKind.Savings -> MochiLemon
    CategoryKind.Other -> Color(0xFF75808A)
}

private fun signedCurrency(transaction: UpiTransactionUi): String {
    val prefix = if (transaction.isIncoming) "+" else "-"
    return prefix + formatCurrency(transaction.amountPaise.absoluteValue)
}
