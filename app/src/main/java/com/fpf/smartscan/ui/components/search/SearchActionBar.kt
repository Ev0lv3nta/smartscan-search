package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SearchActionBar(
    searchEnabled: Boolean,
    onSearch: () -> Unit,
    onShare: () -> Unit,
    onAddTag: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button (
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { onShare() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share",
                )
                Text("Share", style = MaterialTheme.typography.labelMedium)
            }
        }

            Button(
                enabled = searchEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onSearch() }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                    )
                    Text(
                        "Search",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

        Button (
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { onCopy() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally){
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                )
                Text("Copy", style = MaterialTheme.typography.labelMedium)
            }
        }
        Button (
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { onAddTag() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally){
                Icon(
                    Icons.Filled.Tag,
                    contentDescription = "Add tag",
                )
                Text("Add tag", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
