package io.github.ev0lv3nta.smartscansearch.ui.state

import android.net.Uri
import io.github.ev0lv3nta.smartscansearch.media.MediaItem
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import io.github.ev0lv3nta.smartscansearch.ui.state.common.Selectable
import io.github.ev0lv3nta.smartscansearch.ui.state.common.SelectionState

data class SearchState(
    val searchResults: List<MediaItem> = emptyList(),
    val totalResults: Int = 0,
    val mediaType: MediaType = MediaType.IMAGE,
    val queryImage: Uri? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: MediaItem? = null,
    val imageEmbedderLastUsage: Long? = null,
    val textEmbedderLastUsage: Long? = null,
    val tagFilter: String? = null,
    val startDateFilter: Long? = null,
    val endDateFilter: Long? = null,
    val tagOnlySearch: Boolean = false,
    override val selection: SelectionState<MediaItem> = SelectionState()
): Selectable<MediaItem>