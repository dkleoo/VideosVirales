package com.example.videosvirales.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun VideosViralesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = ColorApp.Primary,
    onPrimary = ColorApp.OnPrimary,
    secondary = ColorApp.Secondary,
    background = ColorApp.Background,
    onBackground = ColorApp.TextPrimary,
    surface = ColorApp.Surface,
    onSurface = ColorApp.TextPrimary,
    onSurfaceVariant = ColorApp.TextSecondary,
    outline = ColorApp.Border,
    error = ColorApp.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorApp.Primary,
    onPrimary = ColorApp.OnPrimary,
    secondary = ColorApp.Secondary,
    background = ColorApp.BackgroundDark,
    onBackground = ColorApp.TextPrimaryDark,
    surface = ColorApp.SurfaceDark,
    onSurface = ColorApp.TextPrimaryDark,
    onSurfaceVariant = ColorApp.TextSecondaryDark,
    outline = ColorApp.BorderDark,
    error = ColorApp.ErrorDark,
)
