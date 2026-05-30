package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaCollection

sealed interface CollectionAction {
    data class MergeCollections(val primaryCollectionName: String, val isNewMergedLabel: Boolean = false): CollectionAction
    data class RenameCollection(val newName: String): CollectionAction
    data class CreateNewTagAndTagClusters(val newName: String): CollectionAction
    data class TagClusters(val tagId: Long): CollectionAction
    data class ToggleSelectedCollection(val collection: MediaCollection): CollectionAction
    data class SetCollectionToView(val collection: MediaCollection?): CollectionAction
    data class SetGroupBySimilarity(val groupBySimilarity: Boolean) : CollectionAction
    data class SetSelectAll(val selectAll: Boolean): CollectionAction
    data object DeleteCollections : CollectionAction
    data object ToggleViewAllCollections: CollectionAction
}