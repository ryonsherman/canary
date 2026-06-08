package com.canary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Suppress("DEPRECATION")
private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = DarkBackground,
    secondary = Blue80,
    onSecondary = DarkBackground,
    error = Red80,
    onError = DarkBackground,
    background = DarkBackground,
    onBackground = Green80,
    surface = DarkSurface,
    onSurface = Green80,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Green80.copy(alpha = 0.7f),
    outline = Green80.copy(alpha = 0.3f),
)

@Suppress("DEPRECATION")
private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = LightBackground,
    secondary = Blue40,
    onSecondary = LightBackground,
    error = Red40,
    onError = LightBackground,
    background = LightBackground,
    onBackground = Green40,
    surface = LightSurface,
    onSurface = Green40,
    surfaceVariant = LightSurface,
    onSurfaceVariant = Green40.copy(alpha = 0.7f),
    outline = Green40.copy(alpha = 0.3f),
)

@Composable
fun CanaryTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
        typography = CanaryTypography,
        content = content,
    )
}
