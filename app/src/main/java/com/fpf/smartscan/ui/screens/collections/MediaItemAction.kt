package com.fpf.smartscan.ui.screens.collections

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem

sealed interface MediaItemAction {
    data class MoveMedia(val destinationCollection: MediaCollection, val clusterId: Long? = null): MediaItemAction
    data class CopyMedia(val clipboard: Clipboard, val context: Context): MediaItemAction
    data class ShareMedia(val context: Context): MediaItemAction
    data class CreateNewTagCollectionAndMove(val newName: String): MediaItemAction
    data class ToggleSelectedMedia(val item: MediaItem): MediaItemAction
    data class SetMediaToView(val context: Context, val item: MediaItem?, val autoOpenInGallery: Boolean? = null, val isSelecting: Boolean = false): MediaItemAction
    data class SetCollectionToView(val name: String?, val clusterId: Long): MediaItemAction
    data class Tag(val tag: String): MediaItemAction
    data object RemoveMedia : MediaItemAction
}