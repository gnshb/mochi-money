package com.mochimoney.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.mochimoney.app.ui.theme.MochiCoral
import com.mochimoney.app.ui.theme.MochiLemon
import com.mochimoney.app.ui.theme.MochiMint
import com.mochimoney.app.ui.theme.MochiRose
import com.mochimoney.app.ui.theme.MochiSky

@Composable
fun KawaiiBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "kawaii backdrop")
    val drift by transition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val colorScheme = MaterialTheme.colorScheme

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorScheme.background,
                    colorScheme.primaryContainer.copy(alpha = 0.55f),
                    colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    colorScheme.background,
                ),
            ),
        )
        drawSoftBean(
            center = Offset(w * 0.18f + drift, h * 0.16f),
            radius = w * 0.28f,
            color = MochiRose.copy(alpha = 0.22f),
        )
        drawSoftBean(
            center = Offset(w * 0.86f - drift, h * 0.28f),
            radius = w * 0.22f,
            color = MochiSky.copy(alpha = 0.2f),
        )
        drawSoftBean(
            center = Offset(w * 0.78f, h * 0.84f + drift),
            radius = w * 0.26f,
            color = MochiMint.copy(alpha = 0.2f),
        )
        repeat(6) { index ->
            val x = ((index * 73) % 100) / 100f * w
            val y = ((index * 41) % 100) / 100f * h
            drawSparkle(Offset(x, y), (8.dp + (index % 3).dp).toPx(), listOf(MochiLemon, MochiCoral, MochiSky)[index % 3])
        }
    }
}

@Composable
fun MochiMascot(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    mood: MochiMood = MochiMood.Calm,
) {
    val transition = rememberInfiniteTransition(label = "mochi mascot")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = with(LocalDensity.current) { 8.dp.toPx() },
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "bounce",
    )
    val blush by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "blush",
    )
    val sparklePulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "sparkle pulse",
    )
    val sway by transition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "sway",
    )

    Canvas(modifier = modifier.size(size)) {
        val canvasSize = size.toPx()
        val body = Rect(
            left = canvasSize * 0.13f,
            top = canvasSize * 0.2f + bounce,
            right = canvasSize * 0.87f,
            bottom = canvasSize * 0.78f + bounce,
        )
        val shadow = Rect(
            left = canvasSize * 0.22f - bounce * 0.2f,
            top = canvasSize * 0.78f,
            right = canvasSize * 0.78f + bounce * 0.2f,
            bottom = canvasSize * 0.88f,
        )
        drawOval(MochiSky.copy(alpha = 0.14f), topLeft = shadow.topLeft, size = shadow.size)
        rotate(degrees = sway, pivot = body.center) {
            drawMochiBody(body, blush, mood)
            if (mood == MochiMood.Happy) {
                drawMochiPaw(
                    center = Offset(body.right - body.width * 0.08f, body.top + body.height * 0.32f),
                    radius = body.width * 0.065f,
                    color = MochiRose.copy(alpha = 0.38f),
                    lift = bounce * 0.18f,
                )
            }
            drawSparkle(
                center = Offset(body.right - body.width * 0.08f, body.top + body.height * 0.02f),
                radius = body.width * 0.06f * sparklePulse,
                color = MochiLemon,
            )
            drawSparkle(
                center = Offset(body.left + body.width * 0.04f, body.top + body.height * 0.18f),
                radius = body.width * 0.04f * (1.18f - sparklePulse * 0.18f),
                color = MochiSky,
            )
        }
    }
}

enum class MochiMood {
    Calm,
    Happy,
    Focused,
}

@Composable
fun KawaiiScene(
    modifier: Modifier = Modifier,
    mascotSize: Dp = 130.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        KawaiiBackdrop()
        MochiMascot(
            modifier = Modifier,
            size = mascotSize,
            mood = MochiMood.Happy,
        )
        content()
    }
}

private fun DrawScope.drawSoftBean(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius * 0.8f, center.y)
        cubicTo(
            center.x - radius * 0.9f,
            center.y - radius * 0.55f,
            center.x - radius * 0.15f,
            center.y - radius * 0.92f,
            center.x + radius * 0.32f,
            center.y - radius * 0.72f,
        )
        cubicTo(
            center.x + radius * 0.92f,
            center.y - radius * 0.48f,
            center.x + radius * 0.86f,
            center.y + radius * 0.42f,
            center.x + radius * 0.18f,
            center.y + radius * 0.74f,
        )
        cubicTo(
            center.x - radius * 0.38f,
            center.y + radius,
            center.x - radius * 0.9f,
            center.y + radius * 0.58f,
            center.x - radius * 0.8f,
            center.y,
        )
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.28f, center.y - radius * 0.28f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.28f, center.y + radius * 0.28f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.28f, center.y + radius * 0.28f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.28f, center.y - radius * 0.28f)
        close()
    }
    drawPath(path, color.copy(alpha = 0.58f))
}

private fun DrawScope.drawMochiBody(body: Rect, blush: Float, mood: MochiMood) {
    drawRoundRect(
        color = Color.White,
        topLeft = body.topLeft,
        size = body.size,
        cornerRadius = CornerRadius(body.width * 0.28f, body.height * 0.34f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White, Color(0xFFFFEFEF)),
            startY = body.top,
            endY = body.bottom,
        ),
        topLeft = body.topLeft,
        size = body.size,
        cornerRadius = CornerRadius(body.width * 0.28f, body.height * 0.34f),
    )
    drawArc(
        color = MochiRose.copy(alpha = 0.18f),
        startAngle = 18f,
        sweepAngle = 118f,
        useCenter = false,
        topLeft = Offset(body.left + body.width * 0.12f, body.top + body.height * 0.12f),
        size = Size(body.width * 0.72f, body.height * 0.52f),
        style = Stroke(width = body.width * 0.018f),
    )
    drawCircle(
        MochiRose.copy(alpha = 0.32f * blush),
        body.width * 0.08f,
        Offset(body.left + body.width * 0.28f, body.top + body.height * 0.54f),
    )
    drawCircle(
        MochiRose.copy(alpha = 0.32f * blush),
        body.width * 0.08f,
        Offset(body.right - body.width * 0.28f, body.top + body.height * 0.54f),
    )
    drawFace(body, mood)
}

private fun DrawScope.drawMochiPaw(center: Offset, radius: Float, color: Color, lift: Float) {
    drawCircle(color, radius, center.copy(y = center.y - lift))
    drawCircle(color.copy(alpha = 0.8f), radius * 0.32f, center.copy(x = center.x - radius * 0.5f, y = center.y - radius * 0.58f - lift))
    drawCircle(color.copy(alpha = 0.8f), radius * 0.32f, center.copy(y = center.y - radius * 0.76f - lift))
    drawCircle(color.copy(alpha = 0.8f), radius * 0.32f, center.copy(x = center.x + radius * 0.5f, y = center.y - radius * 0.58f - lift))
}

private fun DrawScope.drawFace(body: Rect, mood: MochiMood) {
    val eyeY = body.top + body.height * 0.42f
    when (mood) {
        MochiMood.Calm -> {
            drawArc(
                color = Color(0xFF302722),
                startAngle = 18f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(body.left + body.width * 0.33f, eyeY - body.height * 0.035f),
                size = Size(body.width * 0.1f, body.height * 0.07f),
                style = Stroke(width = body.width * 0.018f),
            )
            drawArc(
                color = Color(0xFF302722),
                startAngle = 18f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(body.right - body.width * 0.43f, eyeY - body.height * 0.035f),
                size = Size(body.width * 0.1f, body.height * 0.07f),
                style = Stroke(width = body.width * 0.018f),
            )
            drawRoundRect(
                color = Color(0xFF302722),
                topLeft = Offset(body.center.x - body.width * 0.06f, body.top + body.height * 0.56f),
                size = Size(body.width * 0.12f, body.height * 0.025f),
                cornerRadius = CornerRadius(20f, 20f),
            )
        }

        MochiMood.Happy -> {
            drawCircle(Color(0xFF302722), body.width * 0.035f, Offset(body.left + body.width * 0.38f, eyeY))
            drawCircle(Color(0xFF302722), body.width * 0.035f, Offset(body.right - body.width * 0.38f, eyeY))
            drawArc(
                color = Color(0xFF302722),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(body.center.x - body.width * 0.085f, body.top + body.height * 0.48f),
                size = Size(body.width * 0.17f, body.height * 0.17f),
                style = Stroke(width = body.width * 0.025f),
            )
        }

        MochiMood.Focused -> {
            drawRoundRect(
                color = Color(0xFF302722),
                topLeft = Offset(body.left + body.width * 0.34f, eyeY - body.height * 0.015f),
                size = Size(body.width * 0.09f, body.height * 0.025f),
                cornerRadius = CornerRadius(20f, 20f),
            )
            drawRoundRect(
                color = Color(0xFF302722),
                topLeft = Offset(body.right - body.width * 0.43f, eyeY - body.height * 0.015f),
                size = Size(body.width * 0.09f, body.height * 0.025f),
                cornerRadius = CornerRadius(20f, 20f),
            )
            drawRoundRect(
                color = Color(0xFF302722),
                topLeft = Offset(body.center.x - body.width * 0.045f, body.top + body.height * 0.55f),
                size = Size(body.width * 0.09f, body.height * 0.04f),
                cornerRadius = CornerRadius(20f, 20f),
            )
        }
    }
}
