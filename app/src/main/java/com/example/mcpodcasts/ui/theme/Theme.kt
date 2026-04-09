package com.example.mcpodcasts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mcpodcasts.data.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlueDark,
    secondary = AquaDark,
    tertiary = CoralDark,
    background = Night,
    surface = Night,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurface,
    surfaceContainerHighest = NightSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    secondary = Aqua,
    tertiary = Coral,
    background = Mist,
    surface = Mist,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9F0FF),
    surfaceContainerHighest = Color(0xFFDDE8FF),
    onSurfaceVariant = Slate,
)

@Composable
fun MCPodcastsTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}