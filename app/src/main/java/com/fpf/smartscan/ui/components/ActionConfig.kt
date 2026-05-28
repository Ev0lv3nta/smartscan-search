package com.fpf.smartscan.ui.components

import androidx.compose.ui.graphics.vector.ImageVector


sealed interface ActionConfig {
    val enabled: Boolean

    data class Button(
        val onClick: () -> Unit,
        override val enabled: Boolean = true,
        val icon: ImageVector? = null,
    ) : ActionConfig

    data class Switch(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val enabled: Boolean = true,
    ) : ActionConfig
}