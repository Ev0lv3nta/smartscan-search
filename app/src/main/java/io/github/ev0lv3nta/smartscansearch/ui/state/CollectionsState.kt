package io.github.ev0lv3nta.smartscansearch.ui.state

import io.github.ev0lv3nta.smartscansearch.media.CollectionType
import io.github.ev0lv3nta.smartscansearch.media.MediaCollection
import io.github.ev0lv3nta.smartscansearch.ui.state.common.Selectable
import io.github.ev0lv3nta.smartscansearch.ui.state.common.SelectionState

data class CollectionsState(
    val collectionType: CollectionType = CollectionType.CLUSTER,
    val showAllCollections: Boolean = false,
    val loading: Boolean = false,
    val collectToView: MediaCollection? = null,
    val totalCollections: Int = 0,
    override val selection: SelectionState<MediaCollection> = SelectionState()
): Selectable<MediaCollection>