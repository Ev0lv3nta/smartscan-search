package io.github.ev0lv3nta.smartscansearch.search

import android.net.Uri
import io.github.ev0lv3nta.smartscansearch.media.MediaType

sealed interface SearchQuery{
    data class ImageQuery(val uri: Uri, val mediaType: MediaType): SearchQuery
    data class TextQuery(val text: String, val mediaType: MediaType): SearchQuery
}