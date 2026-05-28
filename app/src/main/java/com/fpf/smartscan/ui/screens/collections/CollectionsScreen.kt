package com.fpf.smartscan.ui.screens.collections

import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fpf.smartscan.navigation.Routes
import com.fpf.smartscan.ui.components.modals.SelectorModal
import com.fpf.smartscan.ui.components.SlideRevealBox
import com.fpf.smartscan.ui.components.modals.TextInputModal
import com.fpf.smartscan.ui.components.collections.CollectionPicker
import com.fpf.smartscan.ui.components.collections.MediaCollectionsList
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel.Companion.TOP_N
import kotlinx.coroutines.FlowPreview
import com.fpf.smartscan.R
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.events.CollectionEventType
import com.fpf.smartscan.media.MediaCollection.Companion.UNLABELLED_COLLECTION
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.components.ActionBar
import com.fpf.smartscan.ui.components.ActionConfig
import com.fpf.smartscan.ui.components.DropDownMenuWrapper
import org.koin.compose.viewmodel.koinViewModel


@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    onNavigate: (String) -> Unit,
    onTopBarChange: (TopBarState) -> Unit,
    viewModel: CollectionsViewModel = koinViewModel(),
    ) {

    val actionBarHeight = 70

    val state by viewModel.state.collectAsState()
    val clusterCollections by viewModel.clusterCollections.collectAsState()
    val tagCollections by viewModel.tagCollections.collectAsState()

    val collections = if(state.viewAutoCollections) clusterCollections else tagCollections
    val isCollectionVisible = tagCollections.isNotEmpty() && !state.viewAutoCollections || clusterCollections.isNotEmpty() && state.viewAutoCollections

    val context = LocalContext.current

    // actions
    var isSelecting by remember { mutableStateOf(false) }
    var isRenamingCollection by remember { mutableStateOf(false) }
    var isMergingCollections by remember { mutableStateOf(false) }
    var isTaggingClusters by remember { mutableStateOf(false) }
    var isCreatingNewTagAndTaggingClusters by remember { mutableStateOf(false) }
    var isDeletingCollection by remember { mutableStateOf(false) }

    val actionBarActions: Map<String, ActionConfig> = mapOf(
        stringResource(R.string.merge_action) to ActionConfig({ isMergingCollections = true }, enabled = !state.loading),
        stringResource(R.string.rename_action) to ActionConfig( { isRenamingCollection = true }, enabled = state.selectedCollections.size == 1),
        stringResource(R.string.add_tag_action) to ActionConfig({ isTaggingClusters = true }, enabled = state.viewAutoCollections),
        stringResource(R.string.delete_action) to ActionConfig({ isDeletingCollection = true }, enabled = !state.viewAutoCollections)
    )

    // Menu
    var showMenu by remember { mutableStateOf(false) }
    val menuActions: Map<String, ActionConfig> = mapOf(
        stringResource(R.string.group_by_tag_action) to ActionConfig({ viewModel.onAction(CollectionAction.GroupByTag) }),
        stringResource(R.string.group_by_similarity_action) to ActionConfig({ viewModel.onAction(CollectionAction.GroupBySimilarity) })
    )
    val spaceNotAllowedMessage = stringResource(R.string.alert_space_not_allowed)


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
            viewModel.onAction(CollectionAction.SetCollectionToView(null))
        }
    }

    // Handle action result via events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event.type){
                CollectionEventType.MERGE -> {
                    if(event.success){
                        isSelecting = false
                    }
                    else {
                        event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    }
                }
                CollectionEventType.COPY -> {
                    if(event.success){
                        isSelecting = false
                    }
                    else {
                        event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    }
                }

                CollectionEventType.RENAME -> {
                    if(event.success){
                        isSelecting = false
                    }
                    else {
                        event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    }
                }

                CollectionEventType.DELETE -> {
                    if(event.success){
                        isSelecting = false
                    }
                    else {
                        event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    }
                }
            }
        }
    }

    val screenTitle = stringResource(R.string.title_collections)

    LaunchedEffect(Unit) {
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

    BackHandler(enabled = isSelecting) {
        isSelecting = false
        viewModel.clearSelectedCollections()
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
                    .heightIn(max = maxCollapsablePx.dp)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            ){
                Text(
                    text = if (state.viewAutoCollections) "Auto" else "Tags",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if(state.totalCollections >= TOP_N) {
                    TextButton(
                        onClick = {viewModel.onAction(CollectionAction.ToggleViewAllCollections)}
                    ) {
                        Text(
                            text = if (state.showAllCollections) "Show less" else "Show all",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }


            MediaCollectionsList(
                isVisible = isCollectionVisible,
                numGridColumns = 3,
                items = collections,
                isSelecting = isSelecting,
                selectedItems = state.selectedCollections,
                onItemClick = { viewModel.onAction(CollectionAction.SetCollectionToView(it)) },
                onToggleSelected = { viewModel.onAction(CollectionAction.ToggleSelectedCollection(it)) },
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

            ActionBar(
                actions = actionBarActions,
                modifier = Modifier.height(actionBarHeight.dp),
            )
        }
        AnimatedVisibility(
            visible = isTaggingClusters,
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
                onClose = { isTaggingClusters = false },
                onSelectCollection = {
                    viewModel.onAction(CollectionAction.TagClusters(it.id))
                    isTaggingClusters = false
                },
                onCreateNewCollection = {
                    isTaggingClusters = false
                    isCreatingNewTagAndTaggingClusters = true
                }
            )
        }
    }

    TextInputModal(
        isVisible = isRenamingCollection,
        title=stringResource(R.string.rename_action),
        placeholder = stringResource(R.string.placeholders_rename),
        onClose = { isRenamingCollection = false },
        onConfirm = {
                newName -> viewModel.onAction(CollectionAction.RenameCollection(newName))
            isRenamingCollection = false
        },
        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
        onValueChange = {
            if (!it.text.contains(" ")) {
                true
            } else {
                Toast.makeText(context, spaceNotAllowedMessage, Toast.LENGTH_SHORT).show()
                false
            }
        }
    )

    TextInputModal(
        isVisible = isCreatingNewTagAndTaggingClusters,
        title=stringResource(R.string.add_tag_action),
        placeholder = stringResource(R.string.placeholders_add_tag),
        onClose = { isCreatingNewTagAndTaggingClusters = false },
        onConfirm =  {
            viewModel.onAction(CollectionAction.CreateNewTagAndTagClusters(it))
            isCreatingNewTagAndTaggingClusters = false
        },
        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
        onValueChange = {
            if (!it.text.contains(" ")) {
                true
            } else {
                Toast.makeText(context, spaceNotAllowedMessage, Toast.LENGTH_SHORT).show()
                false
            }
        }
    )

    if (isMergingCollections) {
        val labelledCollections: List<String> = state.selectedCollections.sortedByDescending { it.size}.map { it.name }.filterNot { it == UNLABELLED_COLLECTION }
        var useSelectorInput by remember { mutableStateOf(labelledCollections.isNotEmpty()) }

        if (useSelectorInput) {
            SelectorModal(
                isVisible = labelledCollections.isNotEmpty(),
                initialOption = labelledCollections.first(),
                title = stringResource(R.string.merge_action),
                label = stringResource(R.string.collections_primary_collection_label),
                options = labelledCollections,
                onConfirm = { selected ->
                    viewModel.onAction(CollectionAction.MergeCollections(selected))
                    isMergingCollections = false
                },
                onClose = { isMergingCollections = false }
            )
        }else{
            TextInputModal(
                isVisible = true,
                title = stringResource(R.string.merge_action),
                placeholder = stringResource(R.string.placeholders_rename),
                onClose = { isMergingCollections = false },
                onConfirm =  { newName ->
                    viewModel.onAction(CollectionAction.MergeCollections(newName, isNewMergedLabel = true))
                    isMergingCollections = false
                },
                leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
                onValueChange = {
                    if (!it.text.contains(" ")) {
                        true
                    } else {
                        Toast.makeText(context, spaceNotAllowedMessage, Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            )
        }
    }

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
                { Text(stringResource(R.string.cancel_action)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(CollectionAction.DeleteCollections)
                    isDeletingCollection = false
                })
                { Text(stringResource(R.string.confirm_action)) }
            }
        )
    }
}