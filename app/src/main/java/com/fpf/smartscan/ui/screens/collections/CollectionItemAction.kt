package com.fpf.smartscan.ui.screens.collections

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem

sealed interface CollectionItemAction {
    data class MoveMedia(val destinationCollection: MediaCollection, val clusterId: Long? = null): CollectionItemAction
    data class CopyMedia(val clipboard: Clipboard, val context: Context): CollectionItemAction
    data class ShareMedia(val context: Context): CollectionItemAction
    data class CreateNewTagCollectionAndMove(val newName: String): CollectionItemAction
    data class ToggleSelectedMedia(val item: MediaItem): CollectionItemAction
    data class SetMediaToView(val context: Context, val item: MediaItem?, val autoOpenInGallery: Boolean? = null, val isSelecting: Boolean = false): CollectionItemAction
    data class SetCollectionToView(val name: String?, val clusterId: Long): CollectionItemAction
    data class Tag(val tag: String): CollectionItemAction
    data object RemoveMedia : CollectionItemAction
}