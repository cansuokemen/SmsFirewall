package com.example.smsfirewall.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tekil silme animasyonu: bir kağıt parçası ekranın merkezinde belirir,
 * buruşup top haline gelir, parabolik bir yörünge ile sağ alt köşedeki
 * çöp kutusuna düşer, çöp kutusu kısa bir titreşim yapar.
 */
@Composable
fun PaperCrumpleOverlay(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale     = remember { Animatable(1f) }
    val cornerPx  = remember { Animatable(20f) }
    val rotation  = remember { Animatable(0f) }
    val translateX = remember { Animatable(0f) }
    val translateY = remember { Animatable(0f) }
    val alpha     = remember { Animatable(1f) }
    val trashScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Faz 1: yumuşak buruşma (~420ms)
        launch { scale.animateTo(0.50f, animationSpec = tween(420, easing = FastOutSlowInEasing)) }
        launch { cornerPx.animateTo(110f, animationSpec = tween(420, easing = EaseInOut)) }
        launch { rotation.animateTo(28f, animationSpec = tween(420, easing = EaseInOut)) }
        delay(360)

        // Faz 2: parabolik akıcı düşüş (~620ms)
        launch { translateX.animateTo(360f, animationSpec = tween(620, easing = EaseInOut)) }
        launch { translateY.animateTo(580f, animationSpec = tween(620, easing = EaseOut)) }
        launch { rotation.animateTo(680f, animationSpec = tween(620, easing = LinearEasing)) }
        launch { scale.animateTo(0.20f, animationSpec = tween(620, easing = EaseInOut)) }
        delay(580)

        // Faz 3: çöp ağzında yumuşak kaybolma (~280ms)
        launch { scale.animateTo(0f, animationSpec = tween(280, easing = EaseInOut)) }
        launch { alpha.animateTo(0f, animationSpec = tween(280, easing = EaseInOut)) }

        // Çöp pop animasyonu — daha geniş ve yumuşak
        launch {
            trashScale.animateTo(1.20f, animationSpec = tween(180, easing = EaseOut))
            trashScale.animateTo(0.92f, animationSpec = tween(180, easing = EaseInOut))
            trashScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        }
        delay(520)
        onCompleted()
    }

    val tx     = translateX.value
    val ty     = translateY.value
    val sc     = scale.value
    val rot    = rotation.value
    val al     = alpha.value
    val corner = cornerPx.value
    val ts     = trashScale.value

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = tx * this.density
                    translationY = ty * this.density
                    scaleX    = sc
                    scaleY    = sc
                    rotationZ = rot
                    this.alpha = al
                }
                .clip(RoundedCornerShape(corner.dp))
                .background(Color.White.copy(alpha = 0.96f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp)
                .graphicsLayer {
                    scaleX = ts
                    scaleY = ts
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
