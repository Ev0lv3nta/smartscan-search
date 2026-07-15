package io.github.ev0lv3nta.smartscansearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.ev0lv3nta.smartscansearch.ui.theme.ColorSchemeType
import io.github.ev0lv3nta.smartscansearch.ui.theme.DarkColorPalette
import io.github.ev0lv3nta.smartscansearch.ui.theme.LightColorPalette
import io.github.ev0lv3nta.smartscansearch.ui.theme.ThemeManager
import io.github.ev0lv3nta.smartscansearch.ui.theme.ThemeMode


@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val colorSchemeType by ThemeManager.colorScheme.collectAsState()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = when (colorSchemeType) {
        ColorSchemeType.DEFAULT -> if (darkTheme) darkColorScheme() else lightColorScheme()
        ColorSchemeType.SMARTSCAN -> if (darkTheme) DarkColorPalette else LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
