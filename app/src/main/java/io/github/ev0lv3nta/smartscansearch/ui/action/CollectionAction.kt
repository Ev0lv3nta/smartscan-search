package io.github.ev0lv3nta.smartscansearch.ui.action

import io.github.ev0lv3nta.smartscansearch.media.CollectionType
import io.github.ev0lv3nta.smartscansearch.media.MediaCollection

sealed interface CollectionAction {
    data class MergeCollections(val primaryCollectionName: String, val isNewMergedLabel: Boolean = false): CollectionAction
    data class RenameCollection(val newName: String): CollectionAction
    data class ToggleSelectedCollection(val collection: MediaCollection): CollectionAction
    data class SetCollectionToView(val collection: MediaCollection?): CollectionAction
    data class SetCollectionType(val type: CollectionType) : CollectionAction
    data class SetSelectAll(val selectAll: Boolean): CollectionAction
    data object DeleteCollections : CollectionAction
    data object ToggleViewAllCollections: CollectionAction
    data object ToggleSelectionMode: CollectionAction
    data object ClearSelection: CollectionAction
    data object ResetSelection: CollectionAction
}