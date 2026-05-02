package com.example.smsfirewall.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColorScheme = darkColorScheme(
    primary              = Indigo80,
    onPrimary            = IndigoContainerDark,
    primaryContainer     = IndigoContainerDark,
    onPrimaryContainer   = OnIndigoContainerDark,
    secondary            = Mauve80,
    tertiary             = Rose80,
    background           = BackgroundDark,
    surface              = SurfaceDark,
    surfaceVariant       = SurfaceVariantDark,
    onBackground         = OnSurfaceDark,
    onSurface            = OnSurfaceDark,
    onSurfaceVariant     = OnSurfaceVariantDark,
    outline              = OutlineDark,
    outlineVariant       = OutlineVariantDark,
    error                = ErrorDark,
    errorContainer       = ErrorContainerDark,
    onErrorContainer     = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = androidx.compose.ui.graphics.Color.White,
    primaryContainer     = IndigoContainer,
    onPrimaryContainer   = OnIndigoContainer,
    secondary            = Mauve40,
    tertiary             = Rose40,
    background           = BackgroundLight,
    surface              = SurfaceLight,
    surfaceVariant       = SurfaceVariantLight,
    onBackground         = OnSurfaceLight,
    onSurface            = OnSurfaceLight,
    onSurfaceVariant     = OnSurfaceVariantLight,
    outline              = OutlineLight,
    outlineVariant       = OutlineVariantLight,
    error                = ErrorLight,
    errorContainer       = ErrorContainerLight,
    onErrorContainer     = OnErrorContainerLight
)

@Composable
fun SmsFirewallTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
