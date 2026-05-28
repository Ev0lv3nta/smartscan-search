package com.fpf.smartscan.ui.screens.search

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.search.IndexingStatus
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.ActionConfig
import com.fpf.smartscan.ui.components.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.ProgressBar
import com.fpf.smartscan.ui.components.SelectorIconItem
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.pickers.DatePickerModal
import com.fpf.smartscan.ui.components.modals.BottomSheet
import com.fpf.smartscan.ui.components.search.AutoCompleter
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchActionBar
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.search.SearchResults
import com.fpf.smartscan.ui.components.TagAdder
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import com.fpf.smartscan.utils.formatDate
import com.fpf.smartscan.utils.toEpochSeconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max


@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinViewModel(),
    appSettings:  StateFlow<AppSettings>,
    onTopBarChange: (TopBarState) -> Unit,
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
    val isIndexing = imageIndexStatus == IndexingStatus.ACTIVE || videoIndexStatus == IndexingStatus.ACTIVE
    val alertTitle by searchViewModel.alertTitle.collectAsState()
    val alertDescription by searchViewModel.alertDescription.collectAsState()
    var showScanImagesDialog by remember { mutableStateOf(false) }
    var showScanVideosDialog by remember { mutableStateOf(false) }


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
    var tagAutoCompleteTagResults by remember { mutableStateOf<List<String>>(emptyList()) }


    // Dynamic hide animation
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val actionBarHeight = with(density) { 70.dp.toPx() }
    val searchBarHeight = with(density) { (if(state.queryType == QueryType.IMAGE) 200 else 120).dp.toPx() }
    val maxCollapsePx = max(actionBarHeight, searchBarHeight).toInt()

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Menu
    // TODO: rename resources
    var showMenu by remember { mutableStateOf(false) }
    val scanImagesMenuLabel = stringResource(R.string.setting_scan_images)
    val scanVideosMenuLabel = stringResource(R.string.setting_scan_videos)

    val menuActions: Map<String, ActionConfig> = mapOf(
        scanImagesMenuLabel to ActionConfig({ showScanImagesDialog = true }, enabled = !isIndexing),
        scanVideosMenuLabel to ActionConfig({ showScanVideosDialog = true }, enabled=!isIndexing)
    )

    RequestPermissions { _, storageGranted ->
        hasStoragePermission = storageGranted
    }

    val screenTitle = stringResource(R.string.title_search)

    LaunchedEffect(state.hasIndexedImages, state.hasIndexedVideos, state.isRescanning, state.mediaType, hasStoragePermission) {
        val isFirstImageScanNeeded = hasStoragePermission && state.hasIndexedImages == false && (state.mediaType == MediaType.IMAGE)
        val isFirstVideoScanNeeded = hasStoragePermission && state.hasIndexedVideos == false && (state.mediaType == MediaType.VIDEO)

        if(isFirstImageScanNeeded && !state.isRescanning){
            searchViewModel.showIndexAlert()
        }else if(isFirstVideoScanNeeded && !state.isRescanning){
            searchViewModel.showIndexAlert()
        }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.reloadIndex(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.reloadIndex(MediaType.VIDEO)
        }
    }

    LaunchedEffect(state.searchResults) {
        if(state.searchResults.isEmpty()) offset = 0
    }

    LaunchedEffect(Unit) {
        searchViewModel.externalSearch(intentSearchQuery, appSettings.similarityThreshold, appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
    }

    LaunchedEffect(isIndexing) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                actions = {
                    Box{
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "menu"
                            )
                        }
                        DropDownMenuWrapper(
                            expanded = showMenu,
                            actions = menuActions,
                            onClose = {showMenu = false}
                        )
                    }
                }
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            searchViewModel.toggleViewResult(context, null)
            searchViewModel.clearSelectedResults()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { searchViewModel.searchFieldState.text }
            .debounce(50)
            .collectLatest { query: CharSequence ->
                val subStringEnd = searchViewModel.searchFieldState.selection.end
                tagAutoCompleteTagResults = searchViewModel.handleAutoCompletionCheck(query, subStringEnd)
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

    if (showScanImagesDialog || showScanVideosDialog) {
        val media = if (showScanImagesDialog) "images" else "videos"

        AlertDialog(
            onDismissRequest = {
                if (showScanImagesDialog) showScanImagesDialog = false else showScanVideosDialog = false
            },
            title = {
                Text(stringResource(R.string.alert_scan_index_title, media))
            },
            text = {
                Column {
                    Text(stringResource(R.string.alert_scan_index_description))

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            if (showScanImagesDialog) {
                                showScanImagesDialog = false
                                searchViewModel.refreshMediaIndex(MediaType.IMAGE)
                            } else {
                                showScanVideosDialog = false
                                searchViewModel.refreshMediaIndex(MediaType.VIDEO)
                            }
                        }
                    ) {
                        Text("Refresh")
                    }

                    TextButton(
                        onClick = {
                            if (showScanImagesDialog) {
                                showScanImagesDialog = false
                                searchViewModel.rebuildMediaIndex(MediaType.IMAGE)
                            } else {
                                showScanVideosDialog = false
                                searchViewModel.rebuildMediaIndex(MediaType.VIDEO)
                            }
                        }
                    ) {
                        Text("Rebuild")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showScanImagesDialog) showScanImagesDialog = false else showScanVideosDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    TagAdder(
        isVisible = isAddingTag,
        onAddTag = {
            searchViewModel.tagSelectedItems(it)
            isAddingTag = false
        },
        onClose = {
            isAddingTag = false
            searchViewModel.clearSelectedResults()
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
                        .heightIn(max=180.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Column {
                        if (isSelecting) {
                            val text = if (state.selectedResults.isNotEmpty()) "${state.selectedResults.size} Selected" else "Select items"
                            Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.primary)
                        }

                        ImageSearcher(
                            uri = state.queryImage,
                            mediaType = state.mediaType,
                            imageSize = 140.dp,
                            mediaTypeSelectorEnabled = (videoIndexStatus != IndexingStatus.ACTIVE && imageIndexStatus != IndexingStatus.ACTIVE), // prevent switching modes when indexing in progress
                            onSearch = {
                                searchViewModel.search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
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
                            Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SearchBar(
                                modifier = Modifier.weight(1f),
                                searchFieldState = searchViewModel.searchFieldState,
                                enabled = hasStoragePermission && !state.loading && !isIndexing ,
                                onSearch = {
                                    searchViewModel.search(appSettings.similarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
                                    isSelecting = false
                                },
                                onImageSelected = {
                                    searchViewModel.updateSearchImageUri(it)
                                    searchViewModel.updateQueryType(QueryType.IMAGE)
                                    searchViewModel.search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
                                    isSelecting = false
                                },
                                onImagePasted = {
                                    searchViewModel.updateSearchImageUri(it)
                                    searchViewModel.updateQueryType(QueryType.IMAGE)
                                    searchViewModel.search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
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

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 56.dp)
                                    .widthIn(min=42.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        showFilters = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (state.startDateFilter != null || state.endDateFilter != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if(state.startDateFilter != null || state.endDateFilter != null){
                            TextButton(
                                onClick = { searchViewModel.clearDateFilters() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                AutoCompleter(
                    isVisible = tagAutoCompleteTagResults.isNotEmpty() && !isAddingTag,
                    autoCompleteResults = tagAutoCompleteTagResults,
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
                maxCollapsePx = maxCollapsePx,
                onError = searchViewModel::onErrorAsyncImage
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
                        searchViewModel.updateSearchImageUri(state.selectedResults.first().uri)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        isSelecting = false
                        searchViewModel.clearSelectedResults()
                        searchViewModel.search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
                    }
                },
                onShare = {
                    shareMediaMulti(context, state.selectedResults.map{it.uri})
                    isSelecting = false
                    searchViewModel.clearSelectedResults()
                },
                onAddTag = {
                    isAddingTag = true
                    isSelecting = false
                },
                onCopy = {
                    clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", state.selectedResults.first().uri))
                    isSelecting = false
                    searchViewModel.clearSelectedResults()
                },
            )
        }
        state.resultToView?.let { item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                MediaViewer(
                    items = state.searchResults,
                    initialIndex = state.searchResults.indexOf(item),
                    onLoadMore = searchViewModel::onLoadMore,
                    onClose = { searchViewModel.toggleViewResult(context, null)},
                    onUpdateSearchImage = {
                        searchViewModel.updateSearchImageUri(item.uri)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        searchViewModel.search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe, appSettings.duplicateThreshold)
                        searchViewModel.toggleViewResult(context, null)
                    }
                )
            }
        }
        DatePickerModal(
            show = showStartDatePicker,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { y, m, d ->
                val startDate = toEpochSeconds(y, m, d)
                searchViewModel.setStartDateFilter(startDate)
                showStartDatePicker = false
            },
            initialDateMillis = state.startDateFilter?.times(1000)

        )

        DatePickerModal(
            show = showEndDatePicker,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { y, m, d ->
                val endDate = toEpochSeconds(y, m, d)
                searchViewModel.setEndDateFilter(endDate)
                showEndDatePicker = false
            },
            initialDateMillis = state.endDateFilter?.times(1000)
        )
        BottomSheet(
            show = showFilters,
            onDismiss = { showFilters = false }
        ) {
            Text("Filters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start date")
                    TextButton(onClick = { showStartDatePicker = true }) {
                        Text(state.startDateFilter?.let { formatDate(it) } ?: "Any time")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("End date")
                    TextButton(onClick = { showEndDatePicker = true }) {
                        Text(state.endDateFilter?.let { formatDate(it) } ?: "Any time")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton (
                    onClick = {
                        searchViewModel.clearDateFilters()
                    }
                ) {
                    Text("Clear")
                }
            }
        }
    }
}


