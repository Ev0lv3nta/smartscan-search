package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun TagAdder(
    isVisible: Boolean,
    autoCompleteTagResults: List<String>,
    onClose: () -> Unit,
    onAddTag: (String) -> Unit,
    onCheckAutoCompletion: (text: String, subStringEnd: Int, startWithHashtag: Boolean) -> Unit
){
    if(!isVisible) return

    var newTag by remember { mutableStateOf(TextFieldValue(text = "", selection = TextRange(0))) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { newTag.text }
            .debounce(50)
            .collectLatest { value ->
                onCheckAutoCompletion(value, value.length, false)
            }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Add tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    placeholder = { Text( "Enter new tag", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier= Modifier
                        .fillMaxWidth()
                        .onFocusChanged{isFocused = it.isFocused},
                    shape = MaterialTheme.shapes.small,
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                    leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) }
                )
                AutoCompleter(
                    isVisible = autoCompleteTagResults.isNotEmpty() && isFocused,
                    autoCompleteResults = autoCompleteTagResults,
                    query = newTag.text,
                    onSelect = { newTag = TextFieldValue(text = it, selection = TextRange(it.length))},
                    label = "Tags",
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onClose()
            }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAddTag(newTag.text)
            }) {
                Text("Confirm")
            }
        }
    )
}
