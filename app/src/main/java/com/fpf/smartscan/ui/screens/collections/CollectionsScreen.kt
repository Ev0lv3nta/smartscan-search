package com.fpf.smartscan.ui.screens.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.ui.components.collections.CollectionsActionBar
import com.fpf.smartscan.ui.components.collections.MediaCollectionsList
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    collectionsViewModel: CollectionsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val state by collectionsViewModel.state.collectAsState()
    val mediaClusters by collectionsViewModel.mediaClusters.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()

    val collectionsEmpty = state.collections.isEmpty()

    var isSelecting by remember { mutableStateOf(false) }
    val actionBarVisible = isSelecting && state.selectedCollections.isNotEmpty()

    BackHandler(enabled = isSelecting) {
        isSelecting = false
        collectionsViewModel.clearSelectedCollections()
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My collections",
                    style = MaterialTheme.typography.titleMedium,
                )

                TextButton(onClick = {}) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            MediaCollectionsList(
                isVisible = !collectionsEmpty,
                numGridColumns = 3,
                mediaType = state.mediaType,
                items = state.collections,
                totalItems = state.totalCollections,
                isSelecting = isSelecting,
                selectedItems = state.selectedCollections,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.4).toInt(),
                onViewItem = { },
                onLoadMore = {},
                onToggleSelected = collectionsViewModel::toggleSelectedCollection,
                onToggleSelectionMode = {
                    isSelecting = !isSelecting
                },
                onOffsetChange = { },
                maxCollapsePx = 0
            )

            EmptyCollectionScreen(collectionsEmpty)
        }

        if (actionBarVisible) {
            CollectionsActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(70.dp)
                    .zIndex(1f),
                onDelete = {
                    collectionsViewModel.deleteCollection(state.mediaType, state.selectedCollections.first())
                    isSelecting = false
                },
                onMerge = {
                    isSelecting = false
                },
                onRename = {
                    isSelecting = false
                }
            )
        }
    }
}