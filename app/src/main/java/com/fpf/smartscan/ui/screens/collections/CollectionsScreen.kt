package com.fpf.smartscan.ui.screens.collections

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.SelectorModal
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.TextInputModal
import com.fpf.smartscan.ui.components.collections.CollectionsActionBar
import com.fpf.smartscan.ui.components.collections.MediaCollectionsList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow


@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    appSettings: StateFlow<AppSettings>,
    onNavigate: (String) -> Unit,
    viewModel: CollectionsViewModel = viewModel(),
    ) {
    val state by viewModel.state.collectAsState()
    val mediaClusters by viewModel.mediaClusters.collectAsState()
    val mediaCollections by viewModel.mediaCollections.collectAsState()
    val appSettings by appSettings.collectAsState()
    val collectionsEmpty = mediaCollections.isEmpty()

    val context = LocalContext.current

    var isSelecting by remember { mutableStateOf(false) }
    var isRenamingCollection by remember { mutableStateOf(false) }
    var isMergingCollections by remember { mutableStateOf(false) }

    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()

    LaunchedEffect(state.collectToView) {
        state.collectToView?.let{
            onNavigate(Routes.viewCollection(it.name))
            viewModel.setCollectionToView(null)
        }
    }

    BackHandler(enabled = isSelecting) {
        isSelecting = false
        viewModel.clearSelectedCollections()
    }

    TextInputModal(
        isVisible = isRenamingCollection && state.selectedCollections.size == 1,
        title="Rename collection",
        placeholder = "Enter new collection name",
        onClose = {isRenamingCollection = false},
        onConfirm = {
            newName -> viewModel.renameCollection( state.selectedCollections.first(), newName)
                    },
        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
        onValueChange = {
            if (!it.text.contains(" ")) {
                true
            } else {
                Toast.makeText(context, "Spaces are not allowed", Toast.LENGTH_SHORT).show()
                false
            }
        }
    )

    SelectorModal(
        isVisible = isMergingCollections && state.selectedCollections.isNotEmpty(),
        title="Merge collections",
        label = "Primary collection",
        options = state.selectedCollections.map {it.name },
        onConfirm = {
            selected -> viewModel.mergeCollections(state.mediaType, selected, state.selectedCollections.filterNot { it.name == selected })
                    },
        onClose = { isMergingCollections = false }
    )

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
                val text = if (state.selectedCollections.isNotEmpty()) "${state.selectedCollections.size} Selected" else "Select items"
                Text(
                    text,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My collections",
                    style = MaterialTheme.typography.titleMedium,
                )

                TextButton(onClick = {viewModel.toggleViewAllCollections()}) {
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
                items = mediaCollections,
                isSelecting = isSelecting,
                selectedItems = state.selectedCollections,
                onItemClick = viewModel::setCollectionToView,
                onToggleSelected = viewModel::toggleSelectedCollection,
                onToggleSelectionMode = {
                    isSelecting = !isSelecting
                    offset = 0
                },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx
            )

            EmptyCollectionScreen(collectionsEmpty)
        }

        SlideRevealBox(
            isVisible = isSelecting && state.selectedCollections.isNotEmpty(),
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
            CollectionsActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(70.dp)
                    .zIndex(1f),
                onDelete = {
                    viewModel.deleteCollection( state.selectedCollections.first())
                    isSelecting = false
                },
                onMerge = {
                    isMergingCollections = true
                    isSelecting = false
                },
                onRename = {
                    isRenamingCollection = true
                    isSelecting = false
                }
            )
        }
    }
}