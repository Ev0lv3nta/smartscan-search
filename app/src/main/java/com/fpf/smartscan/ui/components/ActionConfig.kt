package com.fpf.smartscan.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class ActionConfig(
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
)