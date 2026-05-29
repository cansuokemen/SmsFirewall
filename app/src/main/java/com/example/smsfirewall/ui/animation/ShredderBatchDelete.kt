package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ShredderBatchOverlay(
    itemCount: Int,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val binScale = remember { Animatable(0.9f) }
    val binAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        binAlpha.animateTo(1f, animationSpec = tween(120))
        progress.animateTo(1f, animationSpec = tween(720))
        binScale.animateTo(1.16f, animationSpec = tween(110))
        binScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        delay(120)
        onCompleted()
    }

    val visibleCount = itemCount.coerceIn(1, 5)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp)
                .fillMaxWidth(0.78f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(visibleCount) { index ->
                val stagger = (index * 0.08f).coerceAtMost(0.28f)
                val itemProgress = ((progress.value - stagger) / (1f - stagger)).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer {
                            translationX = itemProgress * (70f - index * 9f) * density
                            translationY = itemProgress * (410f - index * 18f) * density
                            rotationZ = itemProgress * (8f - index * 3f)
                            scaleX = 1f - itemProgress * 0.42f
                            scaleY = 1f - itemProgress * 0.42f
                            alpha = (1f - itemProgress * 0.82f).coerceAtLeast(0f)
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer {
                    scaleX = binScale.value
                    scaleY = binScale.value
                    alpha = binAlpha.value
                }
                .size(82.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
