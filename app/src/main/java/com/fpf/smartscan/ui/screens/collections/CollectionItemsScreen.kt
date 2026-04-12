package com.fpf.smartscan.ui.screens.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.collections.CollectionItemsList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow


@OptIn(FlowPreview::class)
@Composable
fun CollectionItemsScreen(
    collectionId: Long?,
    appSettings: StateFlow<AppSettings>,
    viewModel: CollectionItemsViewModel = viewModel(),
    ) {
    val state by viewModel.state.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val appSettings by appSettings.collectAsState()
    val collectionsEmpty = mediaItems.isEmpty()

    val context = LocalContext.current

    var isSelecting by remember { mutableStateOf(false) }

    val actionBarVisible = isSelecting && state.selectedMediaItems.isNotEmpty()

    LaunchedEffect(collectionId) {
        collectionId?.let{viewModel.setCollectionId(it)}
    }


    BackHandler(enabled = isSelecting) {
        isSelecting = false
        viewModel.clearSelectedItems()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            CollectionItemsList(
                isVisible = !collectionsEmpty,
                numGridColumns = 3,
                mediaType = state.mediaType,
                items = mediaItems,
                isSelecting = isSelecting,
                selectedItems = state.selectedMediaItems,
                onViewItem = { },
                onToggleSelected = viewModel::toggleSelectedItem,
                onToggleSelectionMode = {
                    isSelecting = !isSelecting
                },
                onOffsetChange = { },
                maxCollapsePx = 0
            )
        }

//        if (actionBarVisible) {
//            CollectionsActionBar(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .height(70.dp)
//                    .zIndex(1f),
//                onDelete = {
//                    viewModel.deleteCollection(state.mediaType, state.selectedCollections.first())
//                    isSelecting = false
//                },
//                onMerge = {
//                    isMergingCollections = true
//                    isSelecting = false
//                },
//                onRename = {
//                    isRenamingCollection = true
//                    isSelecting = false
//                }
//            )
//        }
    }
}