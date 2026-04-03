package com.fpf.smartscan.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.DirectoryPicker
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.CustomSlider
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.ui.components.BackupAndRestore
import com.fpf.smartscan.ui.components.models.ModelsList
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel.Companion.BACKUP_FILENAME
import com.fpf.smartscansdk.core.models.ModelManager
import com.fpf.smartscansdk.core.models.ModelName
import com.fpf.smartscansdk.core.models.ModelRegistry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: String,
    viewModel: SettingsViewModel,
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val importedModelNames by viewModel.importedModels.collectAsState()
    val isBackupLoading by viewModel.isBackupLoading.collectAsState()
    val isRestoreLoading by viewModel.isRestoreLoading.collectAsState()
    val context = LocalContext.current
    val availableModels = ModelRegistry.filter {item -> item.key in listOf(ModelName.ALL_MINILM_L6_V2, ModelName.DINOV2_SMALL)}

    LaunchedEffect(Unit) {
        viewModel.event.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier.padding(16.dp).fillMaxSize()
    ) {
        Column {
            when (type) {
                SettingTypes.THRESHOLD -> {
                    CustomSlider(
                        label = stringResource(R.string.setting_similarity_threshold),
                        minValue = 0.18f,
                        maxValue = 0.28f,
                        initialValue = appSettings.similarityThreshold,
                        onValueChange = { value ->
                            viewModel.updateSimilarityThreshold(value)
                        },
                        description = stringResource(R.string.setting_similarity_threshold_description)
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
                        description = stringResource(R.string.setting_searchable_folders_description)
                    )
                }
                SettingTypes.SEARCHABLE_VID_DIRS -> {
                    DirectoryPicker(
                        directories = appSettings.searchableVideoDirectories,
                        addDirectory = { newDir -> viewModel.addSearchableVideoDirectory(newDir) },
                        deleteDirectory = { newDir -> viewModel.deleteSearchableVideoDirectory(newDir) },
                        description = stringResource(R.string.setting_searchable_folders_description)
                    )
                }
                SettingTypes.BACKUP_RESTORE -> {
                    BackupAndRestore(
                        onBackup = viewModel::backup,
                        onRestore = viewModel::restore,
                        backupFilename = BACKUP_FILENAME,
                        backupLoading = isBackupLoading,
                        restoreLoading = isRestoreLoading
                    )
                }
                else -> {}
            }
        }
    }
}
