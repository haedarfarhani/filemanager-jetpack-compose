package ir.heydar.nexfolder.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

data class DonutSegment(
    val label: String,
    /** Share of the whole ring, 0..1. */
    val fraction: Float,
    val color: Color
)

/**
 * Animated, interactive donut chart drawn with Compose Canvas.
 *
 * - Segments start at 12 o'clock and sweep clockwise.
 * - Tapping a segment reports it via [onSegmentClick]; tapping outside the ring
 *   reports null (deselect). The selected segment grows while the others dim.
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    trackColor: Color = Color.Transparent,
    selectedLabel: String? = null,
    onSegmentClick: ((String?) -> Unit)? = null,
    centerContent: @Composable () -> Unit = {}
) {
    val sweep = remember(segments) { Animatable(0f) }
    LaunchedEffect(segments) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    // Smooth selection transitions (dim others, grow selected) with two shared values
    val dimProgress by animateFloatAsState(
        targetValue = if (selectedLabel == null) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "donut_dim"
    )
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onSegmentClick != null) {
                        Modifier.pointerInput(segments) {
                            detectTapGestures { pos ->
                                onSegmentClick(
                                    hitTest(pos, size.width.toFloat(), size.height.toFloat(), segments, strokePx)
                                )
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(size.width, size.height) - strokePx
            if (diameter <= 0f) return@Canvas
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            val visibleSegments = segments.filter { it.fraction > 0f }
            if (visibleSegments.isEmpty()) {
                if (trackColor != Color.Transparent) {
                    drawArc(
                        color = trackColor,
                        startAngle = 270f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx)
                    )
                }
                return@Canvas
            }

            val grow = dimProgress
            val gap = if (visibleSegments.size > 1) 1.5f else 0f
            var startAngle = 270f
            visibleSegments.forEach { segment ->
                val isSelected = segment.label == selectedLabel
                val alpha = if (selectedLabel == null || isSelected) {
                    1f
                } else {
                    1f - 0.62f * grow
                }
                val width = if (isSelected) strokePx * (1f + 0.18f * grow) else strokePx
                val fullSweep = segment.fraction * 360f
                val animatedSweep = fullSweep * sweep.value
                val drawnSweep = (animatedSweep - gap).coerceAtLeast(0.5f)
                drawArc(
                    color = segment.color.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = drawnSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = width, cap = StrokeCap.Butt)
                )
                startAngle += fullSweep
            }
        }
        centerContent()
    }
}

/** Angle+radius hit test: 0° at 12 o'clock, clockwise. Null when the tap is off-ring. */
private fun hitTest(
    pos: Offset,
    width: Float,
    height: Float,
    segments: List<DonutSegment>,
    strokePx: Float
): String? {
    val cx = width / 2f
    val cy = height / 2f
    val outer = min(width, height) / 2f - strokePx / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val dist = sqrt(dx * dx + dy * dy)
    val inner = outer - strokePx * 1.5f
    if (dist < inner || dist > outer + strokePx * 0.6f) return null

    val visible = segments.filter { it.fraction > 0f }
    if (visible.isEmpty()) return null

    val angleDeg = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0) % 360.0 + 360.0) % 360.0
    var start = 0f
    for (segment in visible) {
        val sweep = segment.fraction * 360f
        if (angleDeg >= start && angleDeg < start + sweep) return segment.label
        start += sweep
    }
    return visible.last().label
}
