package com.fpf.smartscan.ui.components.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMoveRtl
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Share
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
fun CollectionItemsActionBar(
    onMove: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
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
        Button (
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { onRemove() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.RemoveCircle,
                    contentDescription = "Remove",
                )
                Text("Remove", style = MaterialTheme.typography.labelMedium)
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
            onClick = { onMove() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally){
                Icon(
                    Icons.Default.DriveFileMoveRtl,
                    contentDescription = "Move",
                )
                Text("Move", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
