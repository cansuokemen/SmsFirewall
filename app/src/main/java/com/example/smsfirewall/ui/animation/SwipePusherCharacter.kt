package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Parmakla senkronize hareket eden, insan görünümlü itici karakter.
 * progress 0..1: 0 = ekran dışında, 1 = tam görünür ve itme pozunda.
 */
@Composable
fun SwipePusherCharacter(
    progress: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val visible = (clamped / 0.10f).coerceAtMost(1f)
    val pose    = ((clamped - 0.10f) / 0.55f).coerceIn(0f, 1f)

    val sway = rememberInfiniteTransition(label = "pusherSway")
    val swayAngle by sway.animateFloat(
        initialValue  = -2f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(380, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )

    val skin     = Color(0xFFF1C9A5)
    val hair     = Color(0xFF3B2A24)
    val shirt    = MaterialTheme.colorScheme.primary
    val pants    = Color(0xFF334155)
    val shoes    = Color(0xFF1E1E1E)
    val mouth    = Color(0xFF8B3E2F)
    val eyeColor = Color(0xFF1B1B1B)

    Canvas(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer {
                alpha     = visible
                rotationZ = swayAngle * (0.6f + pose * 0.4f)
            }
    ) {
        val w = size.width
        val h = size.height

        val headRadius = w * 0.18f
        val headCenter = Offset(w * 0.50f, h * 0.24f)

        drawArc(
            color    = hair,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter  = false,
            topLeft    = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.05f),
            size       = Size(headRadius * 2.1f, headRadius * 2.1f),
            style      = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )

        drawCircle(
            color  = skin,
            radius = headRadius,
            center = headCenter
        )

        val eyeY = headCenter.y - headRadius * 0.05f
        drawCircle(color = eyeColor, radius = w * 0.018f, center = Offset(headCenter.x - headRadius * 0.42f, eyeY))
        drawCircle(color = eyeColor, radius = w * 0.018f, center = Offset(headCenter.x + headRadius * 0.42f, eyeY))

        val mouthPath = Path().apply {
            moveTo(headCenter.x - headRadius * 0.30f, headCenter.y + headRadius * 0.40f)
            quadraticTo(
                headCenter.x, headCenter.y + headRadius * (0.55f + pose * 0.10f),
                headCenter.x + headRadius * 0.30f, headCenter.y + headRadius * 0.40f
            )
        }
        drawPath(path = mouthPath, color = mouth, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))

        val torsoTopLeft = Offset(w * 0.34f, h * 0.40f)
        val torsoSize    = Size(w * 0.32f, h * 0.30f)
        drawRoundRect(
            color    = shirt,
            topLeft  = Offset(torsoTopLeft.x, torsoTopLeft.y - h * 0.02f),
            size     = torsoSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f)
        )

        val shoulderL = Offset(torsoTopLeft.x + w * 0.02f, torsoTopLeft.y + h * 0.04f)
        val shoulderR = Offset(torsoTopLeft.x + torsoSize.width - w * 0.02f, torsoTopLeft.y + h * 0.04f)

        val pushReach = pose
        val handL = Offset(
            shoulderL.x - w * (0.06f + 0.18f * pushReach),
            shoulderL.y + h * (0.10f - 0.04f * pushReach)
        )
        val handR = Offset(
            shoulderR.x - w * (0.02f + 0.10f * pushReach),
            shoulderR.y + h * (0.16f - 0.06f * pushReach)
        )

        drawLine(
            color = skin,
            start = shoulderL,
            end   = handL,
            strokeWidth = w * 0.075f,
            cap   = StrokeCap.Round
        )
        drawLine(
            color = skin,
            start = shoulderR,
            end   = handR,
            strokeWidth = w * 0.075f,
            cap   = StrokeCap.Round
        )

        drawCircle(color = skin, radius = w * 0.038f, center = handL)
        drawCircle(color = skin, radius = w * 0.038f, center = handR)

        val hipY = torsoTopLeft.y + torsoSize.height
        val legSpread = w * (0.06f + 0.04f * pose)
        val legBottomY = h * 0.92f
        drawLine(
            color = pants,
            start = Offset(torsoTopLeft.x + torsoSize.width * 0.30f, hipY),
            end   = Offset(torsoTopLeft.x + torsoSize.width * 0.30f - legSpread, legBottomY),
            strokeWidth = w * 0.075f,
            cap   = StrokeCap.Round
        )
        drawLine(
            color = pants,
            start = Offset(torsoTopLeft.x + torsoSize.width * 0.70f, hipY),
            end   = Offset(torsoTopLeft.x + torsoSize.width * 0.70f + legSpread, legBottomY),
            strokeWidth = w * 0.075f,
            cap   = StrokeCap.Round
        )

        drawRoundRect(
            color    = shoes,
            topLeft  = Offset(torsoTopLeft.x + torsoSize.width * 0.30f - legSpread - w * 0.05f, legBottomY - h * 0.02f),
            size     = Size(w * 0.10f, h * 0.05f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawRoundRect(
            color    = shoes,
            topLeft  = Offset(torsoTopLeft.x + torsoSize.width * 0.70f + legSpread - w * 0.05f, legBottomY - h * 0.02f),
            size     = Size(w * 0.10f, h * 0.05f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )

        val effortAlpha = (pose * 0.8f).coerceAtMost(1f)
        if (effortAlpha > 0f) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.04f, w * 0.04f), 0f)
            drawLine(
                color = Color.White.copy(alpha = effortAlpha),
                start = Offset(handL.x - w * 0.12f, handL.y - h * 0.04f),
                end   = Offset(handL.x - w * 0.02f, handL.y - h * 0.02f),
                strokeWidth = w * 0.012f,
                pathEffect  = dashEffect
            )
            drawLine(
                color = Color.White.copy(alpha = effortAlpha),
                start = Offset(handL.x - w * 0.10f, handL.y + h * 0.02f),
                end   = Offset(handL.x - w * 0.02f, handL.y + h * 0.02f),
                strokeWidth = w * 0.012f,
                pathEffect  = dashEffect
            )
        }
    }
}
