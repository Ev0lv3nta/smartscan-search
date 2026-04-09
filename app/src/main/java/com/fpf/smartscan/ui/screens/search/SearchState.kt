package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.search.TagSuggestionsResult

data class SearchState(
    val searchResults: List<Uri> = emptyList(),
    val totalResults: Int = 0,
    val mediaType: MediaType = MediaType.IMAGE,
    val queryType: QueryType = QueryType.TEXT,
    val queryImage: Uri? = null,
    val hasIndexedImages: Boolean? = null,
    val hasIndexedVideos: Boolean? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: Uri? = null,
    val selectedResults: List<Uri> = emptyList(),
    val imageEmbedderLastUsage: Long? = null,
    val textEmbedderLastUsage: Long? = null,
    val autoCompleteTagResults: List<String> = emptyList(),
    val tagFilter: String? = null,
    val suggestedTags: TagSuggestionsResult = TagSuggestionsResult(),
    val tagOnlySearch: Boolean = false,
)