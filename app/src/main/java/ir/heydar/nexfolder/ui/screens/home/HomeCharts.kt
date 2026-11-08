package ir.heydar.nexfolder.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Animated ring gauge drawn with a gradient sweep and a soft glow behind it.
 * Starts at 12 o'clock and fills clockwise.
 */
@Composable
fun StorageRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    gradient: List<Color>,
    trackColor: Color,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "storage_ring"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(size.width, size.height) - strokePx
            if (diameter <= 0f) return@Canvas
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 270f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animated > 0.001f) {
                val brush = Brush.sweepGradient(
                    colors = gradient,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                // Soft glow under the main stroke
                drawArc(
                    brush = brush,
                    startAngle = 270f,
                    sweepAngle = animated * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    alpha = 0.18f,
                    style = Stroke(width = strokePx * 2.1f, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = brush,
                    startAngle = 270f,
                    sweepAngle = animated * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/**
 * Segmented (tick-style) usage bar: each segment lights up as usage grows,
 * with a horizontal gradient across the filled part.
 */
@Composable
fun SegmentedUsageBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 24,
    height: Dp = 8.dp,
    activeColors: List<Color>,
    trackColor: Color
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "segmented_usage"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (segments <= 0) return@Canvas
        val gap = (size.width * 0.014f).coerceAtLeast(1f)
        val segmentWidth = ((size.width - gap * (segments - 1)) / segments).coerceAtLeast(1f)
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        val brush = Brush.horizontalGradient(activeColors, startX = 0f, endX = size.width)
        val filledSegments = animated * segments

        for (index in 0 until segments) {
            val left = index * (segmentWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = corner
            )
            val fill = (filledSegments - index).coerceIn(0f, 1f)
            if (fill > 0.02f) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = corner,
                    alpha = fill
                )
            }
        }
    }
}
