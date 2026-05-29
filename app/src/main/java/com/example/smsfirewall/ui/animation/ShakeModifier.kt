package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.shake(trigger: Any?, amplitudeDp: Float = 6f): Modifier = composed {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        val seq = listOf(-amplitudeDp, amplitudeDp, -amplitudeDp * 0.6f, amplitudeDp * 0.4f, 0f)
        seq.forEach { target ->
            offset.animateTo(target, animationSpec = tween(durationMillis = 60))
        }
    }
    graphicsLayer { translationX = offset.value * density }
}
