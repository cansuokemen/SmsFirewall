package com.example.smsfirewall.data.background

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.smsfirewall.R

/**
 * Hazır gradient ve vector pattern arka plan tanımları.
 * Yeni preset eklemek için ilgili listeye yeni `Gradient`/`Pattern`
 * elemanı ekleyin; tüm UI bileşenleri otomatik olarak yansıtır.
 */
object BackgroundPresets {

    data class GradientPreset(
        val id: String,
        val displayName: String,
        val colors: List<Color>,
        val isVertical: Boolean = true
    )

    data class PatternPreset(
        val id: String,
        val displayName: String,
        @DrawableRes val drawableRes: Int
    )

    val gradients: List<GradientPreset> = listOf(
        GradientPreset("aurora",   "Aurora",        listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF7E22CE))),
        GradientPreset("sunset",   "Günbatımı",     listOf(Color(0xFFFF6B6B), Color(0xFFFFA07A), Color(0xFFFFD93D))),
        GradientPreset("ocean",    "Okyanus",       listOf(Color(0xFF0093E9), Color(0xFF80D0C7))),
        GradientPreset("midnight", "Gece Yarısı",   listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))),
        GradientPreset("sakura",   "Sakura",        listOf(Color(0xFFFFC3A0), Color(0xFFFFAFBD))),
        GradientPreset("forest",   "Orman",         listOf(Color(0xFF134E5E), Color(0xFF71B280))),
        GradientPreset("peach",    "Şeftali",       listOf(Color(0xFFED4264), Color(0xFFFFEDBC))),
    )

    val patterns: List<PatternPreset> = listOf(
        PatternPreset("dots",      "Noktalar",   R.drawable.bg_pattern_dots),
        PatternPreset("waves",     "Dalgalar",   R.drawable.bg_pattern_waves),
        PatternPreset("geometric", "Geometrik",  R.drawable.bg_pattern_geometric),
        PatternPreset("grid",      "Izgara",     R.drawable.bg_pattern_grid),
    )

    val solidPalette: List<Color> = listOf(
        Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFE11D48), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFF06B6D4), Color(0xFF6366F1), Color(0xFFEC4899),
        Color(0xFF14B8A6), Color(0xFFF97316), Color(0xFF64748B), Color(0xFF1F2937)
    )

    fun gradientById(id: String): GradientPreset? = gradients.firstOrNull { it.id == id }
    fun patternById(id: String): PatternPreset? = patterns.firstOrNull { it.id == id }

    fun gradientBrush(id: String, dark: Boolean = false): Brush {
        val preset = gradientById(id) ?: gradients.first()
        val colors = if (dark) preset.colors.map { lerp(it, Color.Black, 0.20f) } else preset.colors
        return if (preset.isVertical) {
            Brush.verticalGradient(colors = colors)
        } else {
            Brush.linearGradient(
                colors = colors,
                start = Offset(0f, 0f),
                end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }
}
