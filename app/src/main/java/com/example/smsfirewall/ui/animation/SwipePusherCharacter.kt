package com.example.smsfirewall.ui.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset

/**
 * Sola kaydırılan kart ile birlikte sahneye giren bir stick-figure: kollar
 * ileri uzanmış, balonu sola "iter" pozunda. progress 0..1 swipe ilerlemesi.
 */
@Composable
fun SwipePusherCharacter(
    progress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.onErrorContainer
) {
    val clamped = progress.coerceIn(0f, 1f)
    val sway = rememberInfiniteTransition(label = "pusherSway")
    val swayAngle by sway.animateFloat(
        initialValue = -3f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )

    val entry = (clamped / 0.18f).coerceAtMost(1f)
    val pushPose = ((clamped - 0.18f) / 0.6f).coerceIn(0f, 1f)

    val strokeColor = remember(color) { color }

    Canvas(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer {
                alpha       = entry
                translationX = (1f - entry) * 60f * this.density
                rotationZ    = swayAngle * (0.4f + pushPose * 0.6f)
            }
    ) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)

        val headRadius = w * 0.13f
        val headCenter = Offset(x = w * 0.42f, y = h * 0.22f)

        drawCircle(
            color  = strokeColor,
            radius = headRadius,
            center = headCenter,
            style  = stroke
        )

        val neck     = Offset(x = headCenter.x, y = headCenter.y + headRadius)
        val torsoEnd = Offset(x = neck.x + w * 0.04f * pushPose, y = h * 0.62f)

        drawLine(
            color = strokeColor,
            start = neck,
            end   = torsoEnd,
            strokeWidth = stroke.width,
            cap   = StrokeCap.Round
        )

        val armReach = w * (0.34f + 0.14f * pushPose)
        val armY     = h * (0.40f - 0.02f * pushPose)
        val shoulder = Offset(x = neck.x + w * 0.03f, y = h * 0.36f)
        val handY    = armY
        drawLine(
            color = strokeColor,
            start = shoulder,
            end   = Offset(x = shoulder.x - armReach, y = handY),
            strokeWidth = stroke.width,
            cap   = StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = Offset(x = shoulder.x, y = shoulder.y + h * 0.02f),
            end   = Offset(x = shoulder.x - armReach * 0.92f, y = handY + h * 0.04f),
            strokeWidth = stroke.width,
            cap   = StrokeCap.Round
        )

        val legSpread = w * (0.10f + 0.06f * pushPose)
        drawLine(
            color = strokeColor,
            start = torsoEnd,
            end   = Offset(x = torsoEnd.x - legSpread, y = h * 0.95f),
            strokeWidth = stroke.width,
            cap   = StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = torsoEnd,
            end   = Offset(x = torsoEnd.x + legSpread, y = h * 0.95f),
            strokeWidth = stroke.width,
            cap   = StrokeCap.Round
        )
    }
}
