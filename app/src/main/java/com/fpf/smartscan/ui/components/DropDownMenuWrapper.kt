package com.fpf.smartscan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun DropDownMenuWrapper(
    actions: Map<String, ActionConfig>,
    expanded: Boolean,
    onClose: () -> Unit,
    offset: DpOffset = DpOffset(x = 0.dp, y = (-40).dp),
){
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onClose() },
        offset = offset,
        shape= RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        for (action in actions){
            val label = action.key
            val actionConfig = action.value
            DropdownMenuItem(
                enabled = actionConfig.enabled,
                text = { Text(label) },
                onClick = {
                    onClose()
                    actionConfig.onClick()
                }
            )
        }
    }
}