package com.fpf.smartscan.ui.components.collections

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fpf.smartscan.media.MediaCollection

@Composable
fun CollectionPicker(
    collections: List<MediaCollection>,
    onClose: () -> Unit,
    onSelectCollection: (collection: MediaCollection) -> Unit,
    onCreateNewCollection: (() -> Unit)? = null
) {

    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            dismissOnBackPress = true,
            focusable = true)
    ) {

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
                        onItemClick = { onSelectCollection(it);  },
                    )
                }
                if(onCreateNewCollection != null) {
                    Button(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.8f),
                        onClick = { onCreateNewCollection.invoke() }
                    ) {
                        Text(text = "New collection", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}