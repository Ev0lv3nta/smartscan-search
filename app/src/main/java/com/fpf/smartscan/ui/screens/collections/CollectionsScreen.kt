package com.fpf.smartscan.ui.screens.collections

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.ui.components.modals.SelectorModal
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.modals.TextInputModal
import com.fpf.smartscan.ui.components.collections.CollectionPicker
import com.fpf.smartscan.ui.components.collections.CollectionsActionBar
import com.fpf.smartscan.ui.components.collections.MediaCollectionsList
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel.Companion.TOP_N
import kotlinx.coroutines.FlowPreview
import com.fpf.smartscan.R
import androidx.compose.ui.res.stringResource

@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    onNavigate: (String) -> Unit,
    viewModel: CollectionsViewModel = viewModel(),
    ) {
    val state by viewModel.state.collectAsState()
    val clusterCollections by viewModel.clusterCollections.collectAsState()
    val tagCollections by viewModel.tagCollections.collectAsState()

    val collections = if(state.viewAutoCollections) clusterCollections else tagCollections
    val isCollectionVisible = tagCollections.isNotEmpty() && !state.viewAutoCollections || clusterCollections.isNotEmpty() && state.viewAutoCollections

    val context = LocalContext.current

    var isSelecting by remember { mutableStateOf(false) }
    var isRenamingCollection by remember { mutableStateOf(false) }
    var isMergingCollections by remember { mutableStateOf(false) }
    var isCopyingCollection by remember { mutableStateOf(false) }
    var isDeletingCollection by remember { mutableStateOf(false) }

    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()

    LaunchedEffect(state.collectToView) {
        state.collectToView?.let{
            if(it.isAutoCollection){
                onNavigate(Routes.viewCollection(it.name, it.id))
            }else{
                onNavigate(Routes.viewCollection(it.name))
            }
            viewModel.setCollectionToView(null)
        }
    }

    LaunchedEffect(state.error) {
        if(state.error != null){
            Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            viewModel.resetErrorState()
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
        onConfirm = { newName ->
            if(state.viewAutoCollections){
                viewModel.renameClusterCollection( state.selectedCollections.first(), newName)
            } else{
                viewModel.renameTagCollection( state.selectedCollections.first(), newName)
            }
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
        onConfirm = { selected ->
            if(!state.viewAutoCollections){
                viewModel.mergeCollections(selected, state.selectedCollections.filterNot { it.name == selected })
            }
                    },
        onClose = { isMergingCollections = false }
    )

    if ( isDeletingCollection) {
        val count = state.selectedCollections.size
        val alertTitle = stringResource(R.string.collections_delete_collections_alert_title)
        val alertDescription = stringResource(
            R.string.collections_delete_collections_alert_description,
            count,
            pluralStringResource(R.plurals.collection_count, count)
        )
        AlertDialog(
            onDismissRequest = { },
            title = { Text(alertTitle) },
            text = { Text(alertDescription) },
            dismissButton = {
                TextButton(onClick = { isDeletingCollection = false })
                { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTagCollections( state.selectedCollections)
                    isDeletingCollection = false
                })
                { Text("Confirm") }
            }
        )
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
                val text = if (state.selectedCollections.isNotEmpty()) "${state.selectedCollections.size} Selected" else "Select items"
                Text(
                    text,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(
                        topStart = 12.dp,
                        bottomStart = 0.dp,
                        topEnd = 12.dp,
                        bottomEnd = 0.dp
                    ))
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            bottomStart = 0.dp,
                            topEnd = 12.dp,
                            bottomEnd = 0.dp
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {viewModel.toggleViewAutoCollections()}
                        )
                ) {
                    Text("Auto collections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if(state.viewAutoCollections) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outline
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {viewModel.toggleViewAutoCollections()}
                        )
                ) {
                    Text("Tag collections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if(!state.viewAutoCollections) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if(state.totalCollections >= TOP_N) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { viewModel.toggleViewAllCollections() }) {
                    Text(
                        text = if (state.showAllCollections) "Show less" else "Show all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }else{
                Spacer(modifier = Modifier.height(16.dp))
            }

            MediaCollectionsList(
                isVisible = isCollectionVisible,
                numGridColumns = 3,
                items = collections,
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

            EmptyCollectionScreen(!isCollectionVisible)
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
                    isDeletingCollection = true
                    isSelecting = false
                },
                deleteEnabled = !state.viewAutoCollections,
                onMerge = {
                    isMergingCollections = true
                    isSelecting = false
                },
                mergeEnabled = !state.viewAutoCollections,
                onRename = {
                    isRenamingCollection = true
                    isSelecting = false
                },
                onCopy  = {
                    isCopyingCollection = true
                    isSelecting = false
                },
                copyEnabled = state.viewAutoCollections
            )
        }
        AnimatedVisibility(
            visible = isCopyingCollection,
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
                collections = tagCollections,
                onClose = {
                    isCopyingCollection = false
                    viewModel.clearSelectedCollections()
                          },
                onSelectCollection = {
                    viewModel.copyFromClusterToTagCollection(state.selectedCollections, it)
                    isCopyingCollection = false
                },
                onCreateNewCollection = {viewModel.createNewCollectionAndCopy(state.selectedCollections,it)}
            )
        }
    }
}