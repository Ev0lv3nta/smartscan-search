package io.github.ev0lv3nta.smartscansearch.ui.action

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.Clipboard
import io.github.ev0lv3nta.smartscansearch.media.MediaItem
import io.github.ev0lv3nta.smartscansearch.media.MediaType

sealed interface SearchAction {
    data class Search(val strictness: Float, val dedupeEnabled: Boolean): SearchAction
    data class SetQueryImageAndSearch(val image: Uri, val strictness: Float, val dedupeEnabled: Boolean): SearchAction
    data class ViewResult(val context: Context, val item: MediaItem, val autoOpenInGallery: Boolean? = null): SearchAction
    data class ToggleSelectedResult(val item: MediaItem): SearchAction
    data class TagItems(val tag: String): SearchAction
    data class SetStartDateFilter(val date: Long?): SearchAction
    data class SetEndDateFilter(val date: Long?): SearchAction
    data class SetMediaTypeFilter(val mediaType: MediaType): SearchAction
    data class CopyResult(val clipboard: Clipboard, val context: Context): SearchAction
    data class ShareResults(val context: Context): SearchAction

    data class SetSelectAll(val selectAll: Boolean): SearchAction
    data object RemoveUploadedImage: SearchAction
    data object ClearDateFilters: SearchAction
    data object Reset: SearchAction
    data object ClearResultView: SearchAction
    data object ToggleSelectionMode: SearchAction
    data object ClearSelection: SearchAction
    data object ResetSelection: SearchAction
}