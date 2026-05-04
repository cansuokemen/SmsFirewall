package com.example.smsfirewall.ui.background

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smsfirewall.data.background.BackgroundImageRepository
import com.example.smsfirewall.data.background.BackgroundPresets
import com.example.smsfirewall.data.background.BackgroundSpec

/**
 * Verilen [BackgroundSpec]'i ekrana basar ve içerikteki kartların
 * okunabilirliğini korumak için gerektiğinde hafif bir scrim ekler.
 */
@Composable
fun AppBackground(
    spec: BackgroundSpec,
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState   = spec,
            animationSpec = tween(durationMillis = 520),
            label         = "bgCrossfade"
        ) { current ->
            Box(Modifier.fillMaxSize()) {
                BackgroundLayer(spec = current, isDark = isDark)
                if (current is BackgroundSpec.CustomImage || current is BackgroundSpec.Pattern) {
                    ReadabilityScrim()
                }
            }
        }
        content()
    }
}

@Composable
private fun BackgroundLayer(spec: BackgroundSpec, isDark: Boolean) {
    when (spec) {
        BackgroundSpec.Default -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
        is BackgroundSpec.Gradient -> {
            val brush = remember(spec.presetId, isDark) {
                BackgroundPresets.gradientBrush(spec.presetId, dark = isDark)
            }
            val driftTransition = rememberInfiniteTransition(label = "gradDrift")
            val driftPhase by driftTransition.animateFloat(
                initialValue   = 0f,
                targetValue    = 1f,
                animationSpec  = infiniteRepeatable(
                    animation  = tween(durationMillis = 14000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "gradDriftPhase"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Hafif yukarı-aşağı + sağa-sola kayma
                        translationY = (driftPhase - 0.5f) * size.height * 0.10f
                        translationX = (driftPhase - 0.5f) * size.width * 0.06f
                        scaleX       = 1.18f
                        scaleY       = 1.18f
                    }
                    .background(brush)
            )
        }
        is BackgroundSpec.SolidColor -> Box(
            Modifier.fillMaxSize().background(Color(spec.argb))
        )
        is BackgroundSpec.Pattern -> PatternBackground(patternId = spec.patternId)
        is BackgroundSpec.CustomImage -> {
            val context = LocalContext.current
            val repo = remember(context) { BackgroundImageRepository(context) }
            val file = remember(spec.fileName) { repo.fileFor(spec.fileName) }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun PatternBackground(patternId: String) {
    val preset = BackgroundPresets.patternById(patternId) ?: return
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val bg = MaterialTheme.colorScheme.background
    val painter = rememberVectorPainter(image = ImageVector.vectorResource(id = preset.drawableRes))
    val tileSize = remember(painter.intrinsicSize) {
        if (painter.intrinsicSize.isSpecified) painter.intrinsicSize else Size(96f, 96f)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                val tw = tileSize.width
                val th = tileSize.height
                if (tw <= 0f || th <= 0f) return@drawBehind
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        translate(left = x, top = y) {
                            with(painter) {
                                draw(size = tileSize, colorFilter = ColorFilter.tint(tint))
                            }
                        }
                        x += tw
                    }
                    y += th
                }
            }
    )
}


@Composable
private fun ReadabilityScrim() {
    val bg = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        bg.copy(alpha = 0.20f),
                        bg.copy(alpha = 0.45f)
                    )
                )
            )
    )
}

@Composable
private fun isAppInDarkTheme(): Boolean {
    val onBg = MaterialTheme.colorScheme.onBackground
    return (onBg.red + onBg.green + onBg.blue) / 3f > 0.5f
}
