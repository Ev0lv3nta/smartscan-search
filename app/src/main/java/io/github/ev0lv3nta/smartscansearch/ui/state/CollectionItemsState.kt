package io.github.ev0lv3nta.smartscansearch.ui.state

import io.github.ev0lv3nta.smartscansearch.media.MediaCollection
import io.github.ev0lv3nta.smartscansearch.media.MediaItem
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import io.github.ev0lv3nta.smartscansearch.ui.state.common.Selectable
import io.github.ev0lv3nta.smartscansearch.ui.state.common.SelectionState

data class CollectionItemsState(
    val collection: MediaCollection? = null,
    val mediaType: MediaType? = null,
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    override val selection: SelectionState<MediaItem> = SelectionState()
): Selectable<MediaItem>