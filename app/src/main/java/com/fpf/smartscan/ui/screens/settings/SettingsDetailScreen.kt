package com.fpf.smartscan.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.pickers.DirectoryPicker
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.common.CustomSlider
import com.fpf.smartscan.settings.SettingTypes
import com.fpf.smartscan.events.BackupEventType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.components.models.ModelsList
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelName
import com.fpf.smartscansdk.ml.models.ModelRegistry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: String,
    viewModel: SettingsViewModel,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
    onRestartApp: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val importedModelNames by viewModel.importedModels.collectAsState()
    val context = LocalContext.current
    val availableModels = ModelRegistry.filter {item -> item.key in listOf(ModelName.ALL_MINILM_L6_V2, ModelName.DINOV2_SMALL)}

    LaunchedEffect(Unit) {
        viewModel.backupEvent.collect { event ->
            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            when(event.type){
                BackupEventType.RESTORE -> if(event.success) onRestartApp()
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.modelEvent.collect { event ->
            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    val screenTitle = when (type) {
        SettingTypes.THRESHOLD -> stringResource(R.string.setting_similarity_threshold)
        SettingTypes.MODELS -> stringResource(R.string.setting_models)
        SettingTypes.MANAGE_MODELS -> stringResource(R.string.setting_manage_models)
        SettingTypes.SEARCHABLE_IMG_DIRS -> stringResource(R.string.setting_searchable_image_folders)
        SettingTypes.SEARCHABLE_VID_DIRS -> stringResource(R.string.setting_searchable_video_folders)
        else -> ""
    }

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        )
    }


    Box(
        modifier = Modifier.padding(16.dp).fillMaxSize()
    ) {
        Column {
            when (type) {
                SettingTypes.THRESHOLD -> {
                    CustomSlider(
                        label = stringResource(R.string.setting_similarity_threshold_label, "text queries"),
                        minValue = 0.18f,
                        maxValue = 0.28f,
                        initialValue = appSettings.similarityThreshold,
                        onValueChange = { value ->
                            viewModel.updateSimilarityThreshold(value)
                        },
                    )
                    CustomSlider(
                        label = stringResource(R.string.setting_similarity_threshold_label, "image queries"),
                        minValue = 0.4f,
                        maxValue = 0.8f,
                        initialValue = appSettings.imageSimilarityThreshold,
                        onValueChange = { value ->
                            viewModel.updateImageSimilarityThreshold(value)
                        },
                    )
                }
                SettingTypes.MODELS -> {
                    ModelsList(
                        importedModels = importedModelNames,
                        availableModels = availableModels.values.toList(),
                        onDownload = {url -> ModelManager.downloadModelExternal(context, url)},
                        onDelete = viewModel::onDeleteModel,
                        onImport=viewModel::onImportModel
                    )
                }

                SettingTypes.SEARCHABLE_IMG_DIRS -> {
                    DirectoryPicker(
                        directories = appSettings.searchableImageDirectories,
                        addDirectory = { newDir -> viewModel.addSearchableImageDirectory(newDir) },
                        deleteDirectory = { newDir -> viewModel.deleteSearchableImageDirectory(newDir) },
                    )
                }
                SettingTypes.SEARCHABLE_VID_DIRS -> {
                    DirectoryPicker(
                        directories = appSettings.searchableVideoDirectories,
                        addDirectory = { newDir -> viewModel.addSearchableVideoDirectory(newDir) },
                        deleteDirectory = { newDir -> viewModel.deleteSearchableVideoDirectory(newDir) },
                    )
                }
                else -> {}
            }
        }
    }
}
