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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.ui.components.CircularCheckbox
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SearchResults(
    isVisible: Boolean,
    searchResults: List<Uri>,
    selectedResults: List<Uri>,
    onViewResult: (uri: Uri?) -> Unit,
    queryType: QueryType,
    onLoadMore: () -> Unit,
    totalResults: Int,
    onToggleSelected: (Uri) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    numGridColumns: Int = 3,
    loadMoreBuffer: Int = 5,
    isSelecting: Boolean = false,
    ) {
    if (!isVisible) return
    val scrollThreshold = 10

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    val actionBarHeight = with(density) { 70.dp.toPx() }
    val searchBarHeight = with(density) { (if(queryType == QueryType.IMAGE) 200 else 120).dp.toPx() }
    val maxCollapsePx = max(actionBarHeight, searchBarHeight).toInt()
    var showScrollToTop by remember { mutableStateOf(false) }
    var totalScrollPx by remember { mutableIntStateOf(0) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaPx = -available.y
                totalScrollPx = (totalScrollPx + deltaPx.roundToInt()).coerceIn(0, maxCollapsePx)

                // update search bar
                onOffsetChange(totalScrollPx)

                // update scroll-to-top button based on total scroll or item index
                val scrollingDown = deltaPx > 0
                showScrollToTop = scrollingDown && (totalScrollPx > scrollThreshold)

                return Offset.Zero
            }
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
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(numGridColumns),
            modifier = Modifier.fillMaxSize().nestedScroll(connection),
            contentPadding = PaddingValues(4.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(0.5.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    )
                    Text(
                        text = "$totalResults Results",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(0.5.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    )
                }
            }

            items(searchResults) { uri ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f))
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if(isSelecting) onToggleSelected(uri) else onViewResult(uri)
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
                    )
                    if(isSelecting) {
                        CircularCheckbox(
                            checked = uri in selectedResults,
                            onCheckedChange = { onToggleSelected(uri) },
                            modifier = Modifier
                                .offset(x = 8.dp, y = 8.dp)
                                .align(Alignment.TopStart),
                        )
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
                    showScrollToTop = false
                    onOffsetChange(0)
                    gridState.scrollToItem(0)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to Top")
            }
        }
    }
}
