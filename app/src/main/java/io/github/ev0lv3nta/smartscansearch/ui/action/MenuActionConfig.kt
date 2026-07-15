package io.github.ev0lv3nta.smartscansearch.ui.action

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface MenuActionConfig {
    val enabled: Boolean

    data class Button(
        val label: String,
        val onClick: () -> Unit,
        override val enabled: Boolean = true,
        val icon: ImageVector? = null,
    ) : MenuActionConfig

    data class Switch(
        val label: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        override val enabled: Boolean = true,
    ) : MenuActionConfig
}