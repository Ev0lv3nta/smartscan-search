package com.fpf.smartscan.ui.components.modals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.google.android.material.carousel.Arrangement
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class)
@Composable
fun TextInputModal(
    isLoading: Boolean? = null,
    isVisible: Boolean,
    title: String,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    placeholder: String = "",
    onValueChange: ((TextFieldValue) -> Boolean)? = null
) {
    if (!isVisible) return

    var value by remember { mutableStateOf(TextFieldValue("", TextRange(0))) }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(title) },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = { newValue ->
                        val allow = onValueChange?.invoke(newValue) ?: true

                        if (!allow) return@TextField

                        value = newValue
                    },
                    placeholder = {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge)
                    },
                    leadingIcon = { leadingIcon?.invoke() }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = isLoading != true,
                onClick = onClose) { Text("Cancel") }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = isLoading != true,
                    onClick = {
                    onConfirm(value.text)
                }) {
                    Text("Confirm")
                }
                if(isLoading == true){
                    LoadingIndicator(
                        isVisible = true,
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    )
}
