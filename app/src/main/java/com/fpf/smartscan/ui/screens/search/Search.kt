package com.fpf.smartscan.ui.screens.search

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
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.zIndex
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.search.ProcessorStatus
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.search.SearchQuery
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
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import kotlin.text.indexOf

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    intentSearchQuery: SearchQuery? = null
) {
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val context = LocalContext.current

    // Index state
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val imageIndexStatus by searchViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by searchViewModel.videoIndexStatus.collectAsState()
    val alertTitle by searchViewModel.alertTitle.collectAsState()
    val alertDescription by searchViewModel.alertDescription.collectAsState()

    // Search state
    val state by searchViewModel.state.collectAsState()
    val imageTags by searchViewModel.allImageTags.collectAsState()
    val videoTags by searchViewModel.allVideoTags.collectAsState()

    var hasStoragePermission by remember { mutableStateOf(false) }
    var visibilityPercent by remember { mutableFloatStateOf(1f) }
    var isAddingTag by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var searchBarPadding = if(visibilityPercent > 0 ) 16 else 0


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
        if (imageIndexStatus == ProcessorStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus == ProcessorStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.VIDEO)
        }
    }

    LaunchedEffect(intentSearchQuery) {
        if(intentSearchQuery == null) return@LaunchedEffect

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                searchViewModel.updateSearchImageUri(intentSearchQuery.uri)
                searchViewModel.updateQueryType(QueryType.IMAGE)
                searchViewModel.imageSearch(appSettings.similarityThreshold)
            }

            is SearchQuery.TextQuery -> {
                searchViewModel.textSearch(intentSearchQuery.text, appSettings.similarityThreshold)
            }
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


    if(isAddingTag) {
        var newTag by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Add tag") },
            text = {
                Column {
                    TextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        placeholder = { Text( "Enter new tag", style = MaterialTheme.typography.bodyLarge) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier= Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddingTag = false
                    searchViewModel.clearSelectedResults()
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.addTag(newTag)
                    isAddingTag = false
                }) {
                    Text("Confirm")
                }
            }
        )
    }

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
                isVisible = imageIndexStatus == ProcessorStatus.ACTIVE,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "Indexing videos ${"%.0f".format(videoIndexProgress * 100)}%",
                isVisible = videoIndexStatus == ProcessorStatus.ACTIVE,
                progress = videoIndexProgress
            )

            if (state.queryType == QueryType.IMAGE) {
                SlideRevealBox(
                    reverse = true,
                    visibilityPercent = visibilityPercent,
                    modifier = Modifier
                        .zIndex(1f)
                        .padding(bottom = searchBarPadding.dp)
                ) {
                    ImageSearcher(
                        uri = state.queryImage,
                        threshold = appSettings.similarityThreshold,
                        mediaType = state.mediaType,
                        imageSize = 120.dp,
                        searchEnabled = state.queryImage != null,
                        mediaTypeSelectorEnabled = (videoIndexStatus != ProcessorStatus.ACTIVE && imageIndexStatus != ProcessorStatus.ACTIVE), // prevent switching modes when indexing in progress
                        onSearch = searchViewModel::imageSearch,
                        onMediaTypeChange = searchViewModel::setMediaType,
                        onRemoveImage = {
                            searchViewModel.updateSearchImageUri(null)
                            searchViewModel.updateQueryType(QueryType.TEXT)
                        }
                    )
                }
            } else {
                SlideRevealBox(
                    reverse = true,
                    visibilityPercent = visibilityPercent,
                    modifier = Modifier
                        .zIndex(1f)
                        .padding(bottom = searchBarPadding.dp)
                ) {
                    SearchBar(
                        searchFieldState = searchViewModel.searchFieldState,
                        enabled = hasStoragePermission && !state.loading,
                        onSearch = searchViewModel::textSearch,
                        onImageSelected = {
                            searchViewModel.updateSearchImageUri(it)
                            searchViewModel.updateQueryType(QueryType.IMAGE)
                        },
                        onImagePasted = {
                            searchViewModel.updateSearchImageUri(it)
                            searchViewModel.updateQueryType(QueryType.IMAGE)
                        },
                        onClearResults = { searchViewModel.reset() },
                        label = when (state.mediaType) {
                            MediaType.IMAGE -> "Search images..."
                            MediaType.VIDEO -> "Search videos..."
                        },
                        threshold = appSettings.similarityThreshold,
                        trailingIcon = {
                            SelectorIconItem(
                                enabled = (videoIndexStatus != ProcessorStatus.ACTIVE && imageIndexStatus != ProcessorStatus.ACTIVE), // prevent switching modes when indexing in progress
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
                AutoCompleter(
                    autoCompleteResults = state.autoCompleteTagResults,
                    query = searchViewModel.searchFieldState.text.toString(),
                    onSelect = searchViewModel::onSelectAutoCompleteResult,
                    label = "Matching tags",
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
                type = state.mediaType,
                searchResults = state.searchResults,
                totalResults=state.totalResults,
                isSelecting = isSelecting,
                selectedResults = state.selectedResults,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.4).toInt(),
                onViewResult = searchViewModel::toggleViewResult,
                onLoadMore = searchViewModel::onLoadMore,
                onToggleSelected = searchViewModel::toggleSelectedResult,
                onToggleSelectionMode = { isSelecting = !isSelecting },
                onActionBarVisibilityPctChange = { visibility -> visibilityPercent = visibility }
            )
        }
        SlideRevealBox(
            isVisible = isSelecting && state.selectedResults.isNotEmpty(),
            visibilityPercent = visibilityPercent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .then(
                    if (visibilityPercent > 0f)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                    else Modifier
                )
            ) {
            SearchActionBar(
                modifier = Modifier.height(70.dp),
                searchEnabled = state.selectedResults.size == 1,
                onSearch = {
                    if (state.selectedResults.size == 1) {
                        searchViewModel.updateSearchImageUri(state.selectedResults[0])
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        isSelecting = false
                        searchViewModel.clearSelectedResults()
                        searchViewModel.imageSearch(appSettings.similarityThreshold)
                    }
                },
                onShare = {
                    shareMediaMulti(context, state.selectedResults)
                    isSelecting = false
                    searchViewModel.clearSelectedResults()
                },
                onAddTag = {
                    isAddingTag = true
                    isSelecting = false
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
                    onClose = { searchViewModel.toggleViewResult(null) },
                    onUpdateSearchImage = {
                        searchViewModel.updateSearchImageUri(uri)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                        searchViewModel.imageSearch(appSettings.similarityThreshold)
                        searchViewModel.toggleViewResult(null)
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

