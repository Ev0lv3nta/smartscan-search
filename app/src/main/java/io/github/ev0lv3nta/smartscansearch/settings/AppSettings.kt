package io.github.ev0lv3nta.smartscansearch.settings

import io.github.ev0lv3nta.smartscansearch.ui.theme.ColorSchemeType
import io.github.ev0lv3nta.smartscansearch.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val textQueryStrictness: Float = 0.4f,
    val imageQueryStrictness: Float = 0.1f,
    val searchableImageDirectories: List<String> = emptyList(),
    val searchableVideoDirectories: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val color: ColorSchemeType = ColorSchemeType.SMARTSCAN,
    val enableDirectGalleryOpen: Boolean = false,
    val resultsPerRow: Int = 4,
    val enableDedupe: Boolean = false,
    )
