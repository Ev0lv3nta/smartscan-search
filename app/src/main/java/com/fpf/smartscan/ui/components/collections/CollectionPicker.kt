package com.fpf.smartscan.ui.components.collections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.ui.components.modals.TextInputModal

@Composable
fun CollectionPicker(
    collections: List<MediaCollection>,
    onClose: () -> Unit,
    onSelectCollection: (collection: MediaCollection) -> Unit,
    onCreateNewCollection: ((name: String) -> Unit)? = null
) {
    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            dismissOnBackPress = true,
            focusable = true
        )
    ) {

        var isCreatingCollection by remember { mutableStateOf(false) }
        val context = LocalContext.current


        TextInputModal(
            isVisible = isCreatingCollection && onCreateNewCollection!= null,
            title="Create collection",
            placeholder = "Enter collection name",
            onClose = {isCreatingCollection = false},
            onConfirm = {
                isCreatingCollection = false
                onClose()
                onCreateNewCollection?.invoke(it)
            },
            leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
            onValueChange = {
                if (!it.text.contains(" ")) {
                    true
                } else {
                    Toast.makeText(context, "Spaces are not allowed", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Pick collection",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    MediaCollectionsList(
                        isVisible = true,
                        numGridColumns = 3,
                        items = collections,
                        onItemClick = { onClose(); onSelectCollection(it);  },
                    )
                }
                if(onCreateNewCollection != null) {
                    Button(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.8f),
                        onClick = { isCreatingCollection = true }
                    ) {
                        Text(text = "New collection", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}