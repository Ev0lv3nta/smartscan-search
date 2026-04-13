package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class)
@Composable
fun SelectorModal(
    isVisible: Boolean,
    title: String,
    options: List<String>,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = "items",
) {
    if (!isVisible) return

    var selectedOption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(title) },
        text = {
            Column {
                SelectorItem(
                    label = label,
                    options = options,
                    selectedOption = selectedOption,
                    onOptionSelected = { selected ->
                        selectedOption = selected
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedOption) }) {
                Text("Confirm")
            }
        }
    )
}
