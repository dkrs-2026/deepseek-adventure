package com.uusse.deepseekadventure.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = DeepSeekBlue,
            background = DarkBg,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
        )
    } else {
        lightColorScheme(
            primary = DeepSeekBlue,
            background = LightBg,
            surface = LightSurface,
            onSurface = LightOnSurface,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
