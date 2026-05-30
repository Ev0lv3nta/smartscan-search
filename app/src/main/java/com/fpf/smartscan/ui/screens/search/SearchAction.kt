package com.fpf.smartscan.ui.screens.search

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.screens.collections.CollectionItemAction

sealed interface SearchAction {
    data class Search(val similarityThreshold: Float, val dedupeEnabled: Boolean, val dedupeThreshold: Float): SearchAction
    data class SetQueryImageAndSearch(val image: Uri, val similarityThreshold: Float, val dedupeEnabled: Boolean, val dedupeThreshold: Float): SearchAction
    data class ViewResult(val context: Context, val item: MediaItem, val autoOpenInGallery: Boolean? = null): SearchAction
    data class ToggleSelectedResult(val item: MediaItem): SearchAction
    data class TagItems(val tag: String): SearchAction
    data class RefreshIndex(val mediaType: MediaType): SearchAction
    data class RebuildIndex(val mediaType: MediaType): SearchAction
    data class SetStartDateFilter(val date: Long?): SearchAction
    data class SetEndDateFilter(val date: Long?): SearchAction
    data class SetMediaTypeFilter(val mediaType: MediaType): SearchAction
    data class CopyResult(val clipboard: Clipboard, val context: Context): SearchAction
    data class ShareResults(val context: Context): SearchAction

    data class SetSelectAll(val selectAll: Boolean): SearchAction
    data object RemoveUploadedImage: SearchAction
    data object ClearDateFilters: SearchAction
    data object Index: SearchAction
    data object Reset: SearchAction
    data object ClearResultView: SearchAction

}