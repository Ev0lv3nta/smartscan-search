package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.components.media.ImageDisplay
import kotlinx.coroutines.launch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import com.fpf.smartscan.ui.components.CircularCheckbox
import kotlinx.coroutines.flow.drop

@Composable
fun SearchResults(
    isVisible: Boolean,
    searchResults: List<Uri>,
    selectedResults: List<Uri>,
    onViewResult: (uri: Uri?) -> Unit,
    type: MediaType,
    onLoadMore: () -> Unit,
    totalResults: Int,
    onToggleSelected: (Uri) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onBottomBarFraction: (Float) -> Unit,
    numGridColumns: Int = 3,
    loadMoreBuffer: Int = 5,
    isSelecting: Boolean = false,
    ) {
    if (!isVisible) return

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var showScrollToTop by remember { mutableStateOf(false) }
    val scrollThreshold = 10

    val density = LocalDensity.current
    val actionBarHeight = 70
    val sensitivityPx = with(density) { actionBarHeight.dp.toPx() }
    var accumulatedPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isSelecting, selectedResults.size) {
        if (isSelecting && selectedResults.isNotEmpty()) {
            accumulatedPx = sensitivityPx
            onBottomBarFraction(1f)
        }
    }

    // Detect scroll to show/hide button
    LaunchedEffect(gridState) {
        var lastIndex = 0
        var lastScrollOffset = 0

        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { (index, offset) ->
                val scrollingUp = index < lastIndex || (index == lastIndex && offset < lastScrollOffset)

                val layoutInfo = gridState.layoutInfo

                // calculate bottom bar visibility
                val firstItemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?: 0
                val deltaPx = (lastIndex - index) * firstItemSize + (lastScrollOffset - offset)
                accumulatedPx = (accumulatedPx + deltaPx).coerceIn(0f, sensitivityPx)
                val fraction = (accumulatedPx / sensitivityPx).coerceIn(0f, 1f)
                onBottomBarFraction(fraction)

                showScrollToTop = !scrollingUp && index > scrollThreshold
                lastIndex = index
                lastScrollOffset = offset
            }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow false
            val lastVisibleItem = visibleItems.last().index
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem + loadMoreBuffer >= totalItems && totalItems < totalResults
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) onLoadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "${searchResults.size} Results",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(4.dp)
            )
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(numGridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(searchResults) { uri ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f))
                            .combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    if(isSelecting){
                                        onToggleSelected(uri)
                                    }else{
                                        onViewResult(uri)
                                    }
                                          },
                                onLongClick = {
                                    if(!isSelecting) {
                                        onToggleSelectionMode()
                                        onToggleSelected(uri)
                                    }
                                }
                            )
                        ) {

                            ImageDisplay(
                                uri = uri,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                type = type
                            )
                            if(isSelecting) {
                                CircularCheckbox(
                                    checked = uri in selectedResults,
                                    onCheckedChange = {
                                        onToggleSelected(uri)
                                    },
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = 8.dp)
                                        .align(Alignment.TopStart),
                                )
                            }
                        }
                    }
                }
            }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(onClick = {
                scope.launch {
                    gridState.scrollToItem(0)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to Top")
            }
        }
    }
}
