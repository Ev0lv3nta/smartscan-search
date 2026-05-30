package com.fpf.smartscan.ui.components.menus

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface MenuItemConfig {
    val enabled: Boolean

    data class Button(
        val label: String,
        val onClick: () -> Unit,
        override val enabled: Boolean = true,
        val icon: ImageVector? = null,
    ) : MenuItemConfig

    data class Switch(
        val label: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val enabled: Boolean = true,
    ) : MenuItemConfig
}