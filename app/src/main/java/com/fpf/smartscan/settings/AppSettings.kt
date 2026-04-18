package com.fpf.smartscan.settings

import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val similarityThreshold: Float = 0.20f,
    val searchableImageDirectories: List<String> = emptyList(),
    val searchableVideoDirectories: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val color: ColorSchemeType = ColorSchemeType.SMARTSCAN,
    val enableDirectGalleryOpen: Boolean = false,
    val resultsPerRow: Int = 4,
    val enableClusterSearch: Boolean = true,
    )
