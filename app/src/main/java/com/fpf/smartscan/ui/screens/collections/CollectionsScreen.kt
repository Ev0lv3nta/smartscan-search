package com.fpf.smartscan.ui.screens.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.FlowPreview


@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    collectionsViewModel: CollectionsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val imageTags by collectionsViewModel.allImageTags.collectAsState()
    val videoTags by collectionsViewModel.allVideoTags.collectAsState()
    val imageClusters by collectionsViewModel.allImageClusters.collectAsState()
    val videoClusters by collectionsViewModel.allVideoCluster.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = false) {
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
            EmptyCollectionScreen((true))
        }
    }
}