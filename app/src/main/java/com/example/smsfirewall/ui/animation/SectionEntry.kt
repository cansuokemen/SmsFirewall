package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SectionEntry(
    index: Int,
    delayPerItemMs: Long = 60L,
    initialOffsetDp: Float = 32f,
    content: @Composable () -> Unit
) {
    val offset = remember { Animatable(initialOffsetDp) }
    val alpha  = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * delayPerItemMs)
        launch {
            offset.animateTo(
                targetValue   = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 320)) }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            translationY = with(density) { offset.value.dp.toPx() }
            this.alpha   = alpha.value
        }
    ) {
        content()
    }
}
