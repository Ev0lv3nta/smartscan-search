package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaCollection

sealed interface CollectionAction {
    data class MergeCollections(val primaryCollectionName: String, val isNewMergedLabel: Boolean = false): CollectionAction
    data class RenameCollection(val newName: String): CollectionAction
    data class CreateNewTagCollectionAndCopy(val newName: String): CollectionAction
    data class CopyFromAutoToTagCollection(val collection: MediaCollection): CollectionAction
    data class ToggleSelectedCollection(val collection: MediaCollection): CollectionAction
    data class SetCollectionToView(val collection: MediaCollection?): CollectionAction
    data object DeleteCollections : CollectionAction
    data object ToggleSelectedCollectionType: CollectionAction
    data object ToggleViewAllCollections: CollectionAction
}