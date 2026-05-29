package com.example.smsfirewall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Yarı saydam, yumuşak konturlu bir "glassmorphism" yüzeyi.
 *
 * Arka planda gradient/görsel olduğunda yüzey altındaki dokunun
 * sızdırılması için surface tonu üzerine alpha uygulanır; ince
 * outline cam kenarı hissi verir.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundAlpha: Float = 0.78f,
    overrideContainerColor: Color? = null,
    border: BorderStroke? = BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    ),
    content: @Composable () -> Unit
) {
    val container = overrideContainerColor
        ?: MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha)
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .let { m -> if (border != null) m.border(border, shape) else m }
    ) {
        content()
    }
}
