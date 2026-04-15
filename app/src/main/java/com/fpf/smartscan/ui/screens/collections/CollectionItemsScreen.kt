package com.fpf.smartscan.ui.screens.collections

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.collections.CollectionItemsActionBar
import com.fpf.smartscan.ui.components.collections.CollectionItemsList
import com.fpf.smartscan.ui.components.collections.CollectionPicker
import com.fpf.smartscan.ui.components.media.MediaViewer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow


@OptIn(FlowPreview::class)
@Composable
fun CollectionItemsScreen(
    collectionName: String?,
    appSettings: StateFlow<AppSettings>,
    clusterId: Long = -1L, // null not allowed for longs in nav
    viewModel: CollectionItemsViewModel = viewModel(),
    ) {

    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val mediaCollections by viewModel.mediaCollections.collectAsState()
    val state by viewModel.state.collectAsState()
    val appSettings by appSettings.collectAsState()

    var isSelecting by remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }

    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()

    val tagCollectionItems = viewModel.tagItems.collectAsLazyPagingItems()
    val clusterCollectionItems = viewModel.clusterItems.collectAsLazyPagingItems()
    val isTagCollection = state.clusterId == -1L


    LaunchedEffect(collectionName, clusterId) {
        viewModel.setCollection(collectionName, clusterId)
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
            verticalArrangement = Arrangement.Top
        ) {
            SlideRevealBox(
                isVisible = isSelecting,
                reverse = true,
                offsetPx = offset,
                modifier = Modifier
                    .zIndex(1f)
                    .heightIn(max=maxCollapsablePx.dp)
                    .padding(bottom = 8.dp)
            ) {
                val text = if (state.selectedMediaItems.isNotEmpty()) "${state.selectedMediaItems.size} Selected" else "Select items"
                Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.primary)
            }
            CollectionItemsList(
                isVisible = tagCollectionItems.itemCount > 0 || clusterCollectionItems.itemCount> 0,
                numGridColumns = appSettings.resultsPerRow,
                items = if(isTagCollection) tagCollectionItems else clusterCollectionItems,
                isSelecting = isSelecting,
                selectedItems = state.selectedMediaItems,
                onViewItem = { uri -> viewModel.setMediaToView(context, uri, appSettings.enableDirectGalleryOpen, isSelecting) },
                onToggleSelected = viewModel::toggleSelectedItem,
                onToggleSelectionMode = {
                    isSelecting = !isSelecting
                    offset = 0
                },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx,
                onError = viewModel::onErrorAsyncImage
            )
        }

        SlideRevealBox(
            isVisible = isSelecting && state.selectedMediaItems.isNotEmpty(),
            offsetPx = offset,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .then(
                    if (offset != 0)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                    else Modifier
                )
        ) {
            CollectionItemsActionBar(
                modifier = Modifier.height(70.dp),
                onRemove = {
                    viewModel.removeItems(state.selectedMediaItems)
                    if(isTagCollection) tagCollectionItems.refresh() else clusterCollectionItems.refresh()
                    isSelecting = false
                },
                removeEnabled = isTagCollection,
                onShare = {
                    shareMediaMulti(context, state.selectedMediaItems.map{it.uri})
                    isSelecting = false
                    viewModel.clearSelectedItems()
                          },
                onMove = {
                    isMoving = true
                    isSelecting = false
                         },
                moveEnabled = isTagCollection,
                onCopy = {
                    clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", state.selectedMediaItems[0].uri))
                    isSelecting = false
                    viewModel.clearSelectedItems()
                }
            )
        }
        state.mediaToView?.let { item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(300)
                )
            ) {
                MediaViewer(
                    uri = item.uri,
                    type = item.type,
                    onClose = { viewModel.setMediaToView(context, null) },
                    onUpdateSearchImage = null
                )
            }
            }
        AnimatedVisibility(
            visible = isMoving,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(500)
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            CollectionPicker(
                collections = mediaCollections,
                onClose = { isMoving = false },
                onSelectCollection = {
                    viewModel.moveItems(state.selectedMediaItems, it)
                    if(isTagCollection) tagCollectionItems.refresh() else clusterCollectionItems.refresh()
                    isMoving = false
                }
            )
        }
    }
}