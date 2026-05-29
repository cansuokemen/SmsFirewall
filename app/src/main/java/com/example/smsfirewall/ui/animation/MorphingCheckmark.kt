package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MorphingCheckmark(
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color.White,
    background: Color = Color.Black.copy(alpha = 0.55f)
) {
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "checkScale"
    )
    val progress by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label         = "checkProgress"
    )
    Canvas(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(background)
    ) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.32f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val total   = measure.length
        val segment = Path()
        measure.getSegment(0f, total * progress, segment, true)
        drawPath(
            path  = segment,
            color = color,
            style = Stroke(width = (w * 0.10f).coerceAtLeast(2f), cap = StrokeCap.Round)
        )
    }
}
