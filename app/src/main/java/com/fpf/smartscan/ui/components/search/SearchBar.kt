package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchBar(
    searchFieldState: TextFieldState,
    enabled: Boolean,
    threshold: Float,
    onSearch: ( threshold: Float) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onImagePasted: (Uri?) -> Unit,
    onClearResults : () -> Unit,
    label: String,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onImageSelected(it) }
        }
    )

    val receiveContentListener = remember {
        ReceiveContentListener { transferableContent ->
            when {
                transferableContent.hasMediaType(MediaType.Image) -> {
                    transferableContent.consume { item ->
                        val uri = item.uri
                        if (uri != null) onImagePasted(uri)
                        uri != null
                    }
                }
                else -> transferableContent
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor = if(isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val tagColor =  MaterialTheme.colorScheme.onSurface.copy(0.5f)
    val ot = OutputTransformation {
        val builder = AnnotatedString.Builder(searchFieldState.text.toString())
        val regex = Regex("^#\\w+")
        regex.findAll(searchFieldState.text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = tagColor, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border( width = 1.dp, color = outlineColor, shape = RoundedCornerShape(8.dp) )
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .contentReceiver(receiveContentListener)
    ) {
        BasicTextField(
            state = searchFieldState,
            interactionSource = interactionSource,
            enabled = enabled,
            lineLimits = TextFieldLineLimits.SingleLine,
            outputTransformation = ot,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            onKeyboardAction = {
                if (searchFieldState.text.isNotBlank()) {
                    onSearch(threshold)
                } else {
                    it()
                }
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().contentReceiver(receiveContentListener),
            decorator = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 56.dp)
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.Top).padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Upload image",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.3f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f).padding(vertical = 16.dp)
                    ) {
                        innerTextField()
                        if (searchFieldState.text.isEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                            )
                        }
                    }

                    if (searchFieldState.text.isNotBlank()) {
                        IconButton(
                            enabled = enabled,
                            onClick = {
                                searchFieldState.clearText()
                                onClearResults()
                                      },
                            modifier = Modifier.align(Alignment.Top).padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear query",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), shape = CircleShape)
                                    .size(16.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.Top).padding(top = 4.dp)){
                        trailingIcon?.invoke()
                    }
                }
            }
        )
    }
}
