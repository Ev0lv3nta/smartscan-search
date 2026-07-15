package io.github.ev0lv3nta.smartscansearch.ui.action

import androidx.compose.ui.graphics.vector.ImageVector

data class ActionConfig(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
)