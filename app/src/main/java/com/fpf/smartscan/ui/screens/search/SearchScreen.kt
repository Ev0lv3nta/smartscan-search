package com.fpf.smartscan.ui.screens.search

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.search.IndexingStatus
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.ProgressBar
import com.fpf.smartscan.ui.components.SelectorIconItem
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.search.AutoCompleter
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchActionBar
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.search.SearchResults
import com.fpf.smartscan.ui.components.search.TagAdder
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max


@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    appSettings:  StateFlow<AppSettings>,
    intentSearchQuery: SearchQuery? = null
) {
    val appSettings by appSettings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    // Index state
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val imageIndexStatus by searchViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by searchViewModel.videoIndexStatus.collectAsState()
    val alertTitle by searchViewModel.alertTitle.collectAsState()
    val alertDescription by searchViewModel.alertDescription.collectAsState()

    // Search state
    val state by searchViewModel.state.collectAsState()
    val tags by searchViewModel.allTags.collectAsState()
    val searchBarPlaceholders = listOf(
        when (state.mediaType) {
            MediaType.IMAGE -> "Search images"
            MediaType.VIDEO -> "Search videos"
        },
        "Search by tag: #tag",
        "Search by tag: #tag query"
    )
    var hasStoragePermission by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val actionBarHeight = with(density) { 70.dp.toPx() }
    val searchBarHeight = with(density) { (if(state.queryType == QueryType.IMAGE) 200 else 120).dp.toPx() }
    val maxCollapsePx = max(actionBarHeight, searchBarHeight).toInt()

    RequestPermissions { _, storageGranted ->
        hasStoragePermission = storageGranted
    }

    LaunchedEffect(state.hasIndexedImages, state.hasIndexedVideos, hasStoragePermission, state.mediaType) {
        if(hasStoragePermission && state.hasIndexedImages == false && (state.mediaType == MediaType.IMAGE)){
            searchViewModel.showIndexAlert()
        }else if(hasStoragePermission && state.hasIndexedVideos == false && (state.mediaType == MediaType.VIDEO)){
            searchViewModel.showIndexAlert()
        }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.VIDEO)
        }
    }

    LaunchedEffect(state.searchResults) {
        if(state.searchResults.isEmpty()) offset = 0
    }

    LaunchedEffect(Unit) {
        searchViewModel.externalSearch(intentSearchQuery, appSettings.similarityThreshold, appSettings.enableClusterSearch)
    }

    DisposableEffect(Unit) {
        onDispose {
            searchViewModel.toggleViewResult(context, null)
            searchViewModel.clearSelectedResults()
        }
    }

    if ( !alertTitle.isNullOrBlank() && !alertDescription.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(alertTitle!!) },
            text = { Text(alertDescription!!) },
            dismissButton = {
                TextButton(onClick = {
                    searchViewModel.clearIndexAlert()
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.clearIndexAlert()
                    searchViewModel.onIndex()
                }) {
                    Text("Confirm")
                }
            }
        )
    }

    TagAdder(
        isVisible = isAddingTag,
        suggestedTags = state.suggestedTags,
        autoCompleteTagResults = state.autoCompleteTagResults,
        onAddTag = {
            searchViewModel.tagSelectedItems(it)
            searchViewModel.updateAutoCompleteResults(emptyList())
            isAddingTag = false
        },
        onClose = {
            isAddingTag = false
            searchViewModel.clearSelectedResults()
            searchViewModel.updateAutoCompleteResults(emptyList())
        },
        onCheckAutoCompletion = searchViewModel::handleAutoCompletionCheck
    )

    BackHandler(enabled = isSelecting) {
        isSelecting = false
        searchViewModel.clearSelectedResults()
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

            ProgressBar(
                label = "Indexing images ${"%.0f".format(imageIndexProgress * 100)}%",
                isVisible = imageIndexStatus == IndexingStatus.ACTIVE,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "Indexing videos ${"%.0f".format(videoIndexProgress * 100)}%",
                isVisible = videoIndexStatus == IndexingStatus.ACTIVE,
                progress = videoIndexProgress
            )
            if (state.queryType == QueryType.IMAGE) {
                SlideRevealBox(
                    reverse = true,
                    offsetPx = offset,
                    modifier = Modifier
                        .zIndex(1f)
                        .heightIn(max=200.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Column {
                        if (isSelecting) {
                            val text = if (state.selectedResults.isNotEmpty()) "${state.selectedResults.size} Selected" else "Select items"
                            Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        ImageSearcher(
                            uri = state.queryImage,
                            mediaType = state.mediaType,
                            imageSize = 160.dp,
                            mediaTypeSelectorEnabled = (videoIndexStatus != IndexingStatus.ACTIVE && imageIndexStatus != IndexingStatus.ACTIVE), // prevent switching modes when indexing in progress
                            onSearch = {
                                searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                                isSelecting = false
                            },
                            onMediaTypeChange = searchViewModel::setMediaType,
                            onRemoveImage = {
                                isSelecting = false
                                searchViewModel.updateSearchImageUri(null)
                                searchViewModel.reset()
                                searchViewModel.updateQueryType(QueryType.TEXT)
                            }
                        )
                    }
                }
            } else {
                SlideRevealBox(
                    reverse = true,
                    offsetPx = offset,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .heightIn(max=120.dp)

                ) {
                    Column {
                        if (isSelecting) {
                            val text = if (state.selectedResults.isNotEmpty()) "${state.selectedResults.size} Selected" else "Select items"
                            Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        SearchBar(
                            searchFieldState = searchViewModel.searchFieldState,
                            enabled = hasStoragePermission && !state.loading,
                            onSearch = {
                                searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                                isSelecting = false
                            },
                            onImageSelected = {
                                searchViewModel.updateSearchImageUri(it)
                                searchViewModel.updateQueryType(QueryType.IMAGE)
                                searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                                isSelecting = false
                            },
                            onImagePasted = {
                                searchViewModel.updateSearchImageUri(it)
                                searchViewModel.updateQueryType(QueryType.IMAGE)
                                searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                                isSelecting = false
                            },
                            onClearResults = {
                                isSelecting = false
                                searchViewModel.reset()
                                             },
                            placeholders = searchBarPlaceholders,
                            trailingIcon = {
                                SelectorIconItem(
                                    enabled = (videoIndexStatus != IndexingStatus.ACTIVE && imageIndexStatus != IndexingStatus.ACTIVE), // prevent switching modes when indexing in progress
                                    label = "Media type",
                                    options = mediaTypeOptions.values.toList(),
                                    selectedOption = mediaTypeOptions[state.mediaType]!!,
                                    onOptionSelected = { selected ->
                                        val newMode = mediaTypeOptions.entries
                                            .find { it.value == selected }
                                            ?.key ?: MediaType.IMAGE
                                        searchViewModel.setMediaType(newMode)
                                    }
                                )
                            }
                        )
                    }
                }
                AutoCompleter(
                    isVisible = state.autoCompleteTagResults.isNotEmpty() && !isAddingTag,
                    autoCompleteResults = state.autoCompleteTagResults,
                    query = searchViewModel.searchFieldState.text.toString(),
                    onSelect = searchViewModel::onSelectAutoCompleteResult,
                    label = "Tags",
                )
            }

                LoadingIndicator(isVisible = state.loading, size = 48.dp, strokeWidth = 4.dp, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp))

            state.error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            if(!hasStoragePermission && !state.loading){
                Text(text = stringResource(R.string.storage_permissions), color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            SearchPlaceholderDisplay(isVisible = state.searchResults.isEmpty())

            SearchResults(
                isVisible = !state.loading && state.searchResults.isNotEmpty(),
                numGridColumns = appSettings.resultsPerRow,
                mediaType = state.mediaType,
                searchResults = state.searchResults,
                totalResults=state.totalResults,
                isSelecting = isSelecting,
                selectedResults = state.selectedResults,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.4).toInt(),
                onViewResult = { uri -> searchViewModel.toggleViewResult(context, uri, autoOpenInGallery = appSettings.enableDirectGalleryOpen, isSelecting = isSelecting ) },
                onLoadMore = searchViewModel::onLoadMore,
                onToggleSelected = searchViewModel::toggleSelectedResult,
                onToggleSelectionMode = {
                    isSelecting = !isSelecting
                    offset = 0
                                        },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsePx
            )
        }
        SlideRevealBox(
            isVisible = isSelecting && state.selectedResults.isNotEmpty(),
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
            SearchActionBar(
                modifier = Modifier.height(70.dp),
                searchEnabled = state.selectedResults.size == 1 && state.mediaType == MediaType.IMAGE,
                onSearch = {
                    if (state.selectedResults.size == 1) {
                        searchViewModel.updateSearchImageUri(state.selectedResults[0])
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        isSelecting = false
                        searchViewModel.clearSelectedResults()
                        searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                    }
                },
                onShare = {
                    shareMediaMulti(context, state.selectedResults)
                    isSelecting = false
                    searchViewModel.clearSelectedResults()
                },
                onAddTag = {
                    searchViewModel.updateSuggestedTags()
                    isAddingTag = true
                    isSelecting = false
                },
                onCopy = {
                    clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", state.selectedResults[0]))
                    isSelecting = false
                    searchViewModel.clearSelectedResults()
                },
            )
        }
        state.resultToView?.let { uri ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                MediaViewer(
                    uri = uri,
                    type = state.mediaType,
                    onClose = { searchViewModel.toggleViewResult(context, null)},
                    onUpdateSearchImage = {
                        searchViewModel.updateSearchImageUri(uri)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        searchViewModel.search(appSettings.similarityThreshold, appSettings.enableClusterSearch)
                        searchViewModel.toggleViewResult(context, null)
                    }
                )
            }
        }
    }
}

@Composable
fun SearchPlaceholderDisplay(isVisible: Boolean) {
    if(!isVisible) return

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ImageSearch,
                contentDescription = "Search icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Text(
                textAlign = TextAlign.Center,
                text = "Find what you're looking for",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

