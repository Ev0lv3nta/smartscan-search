package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionHeaderRow(
    selectedCount: Int,
    checked: Boolean,
    onSelectAllChange: (Boolean) -> Unit
) {
    val text = if (selectedCount > 0) "$selectedCount Selected" else "Select items"

    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularCheckbox(
            checked = checked,
            onCheckedChange = onSelectAllChange
        )

        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}