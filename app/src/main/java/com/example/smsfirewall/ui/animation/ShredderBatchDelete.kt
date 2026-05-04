package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Toplu silme animasyonu: seçili kart placeholder'ları öğütücü çizgisine kayar,
 * geçtikten sonra dikey şeritlere ayrılıp aşağıya düşer, çöp kutusu shake atar.
 */
@Composable
fun ShredderBatchOverlay(
    itemCount: Int,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val approach = remember { Animatable(0f) }      // 0..1 öğütücüye yaklaşma
    val shredProgress = remember { Animatable(0f) } // 0..1 şerit düşüşü
    val finalDrop = remember { Animatable(0f) }     // 0..1 çöpe akma
    val trashScale = remember { Animatable(1f) }
    val cogRotation = rememberInfiniteTransition(label = "shredderCogs")
    val cogAngle by cogRotation.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cogAngle"
    )

    LaunchedEffect(Unit) {
        launch { approach.animateTo(1f, animationSpec = tween(450)) }
        delay(220)
        launch { shredProgress.animateTo(1f, animationSpec = tween(520)) }
        delay(420)
        launch { finalDrop.animateTo(1f, animationSpec = tween(380)) }
        delay(280)
        launch {
            trashScale.animateTo(1.22f, animationSpec = tween(120))
            trashScale.animateTo(0.92f, animationSpec = tween(110))
            trashScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(360)
        onCompleted()
    }

    val visibleCount = itemCount.coerceIn(1, 6)

    Box(modifier = modifier.fillMaxSize()) {
        // Öğütücüye yaklaşan kart placeholder'lar (üst bölge)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .fillMaxWidth(0.78f)
                .graphicsLayer {
                    translationY = approach.value * 220f * this.density
                    alpha        = (1f - shredProgress.value).coerceAtLeast(0f)
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(visibleCount) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            translationY = (index * 6f) * this.density
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                )
            }
        }

        // Öğütücü çizgisi + dişliler
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .height(34.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(28.dp)
                    .graphicsLayer { rotationZ = cogAngle }
            )
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
                    .graphicsLayer { rotationZ = -cogAngle }
            )
        }

        // Şeritler — öğütücüden çıkıp aşağıya düşen 6 dikey çubuk
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f)
                .height(220.dp)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(6) { idx ->
                    val phase = idx / 6f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val sp = (shredProgress.value - phase * 0.15f).coerceIn(0f, 1f)
                                translationY = sp * 180f * this.density
                                rotationZ    = (idx - 2.5f) * sp * 6f
                                alpha        = (1f - finalDrop.value * 0.7f).coerceAtLeast(0f)
                            }
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    )
                }
            }
        }

        // Çöp kutusu (sağ alt)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp)
                .size(72.dp)
                .graphicsLayer {
                    scaleX = trashScale.value
                    scaleY = trashScale.value
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
