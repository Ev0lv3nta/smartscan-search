package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.utils.copyFromUri
import com.fpf.smartscan.utils.copyToUri
import com.fpf.smartscan.utils.hashFile
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.settings.saveSettings
import com.fpf.smartscan.utils.unzipFiles
import com.fpf.smartscan.utils.zipFiles
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import com.fpf.smartscansdk.core.models.ModelInfo
import com.fpf.smartscansdk.core.models.ModelManager
import com.fpf.smartscansdk.core.models.ModelName
import com.fpf.smartscansdk.core.models.ModelRegistry
import com.fpf.smartscansdk.core.processors.Metrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _importedModels = MutableStateFlow<List<ModelName>>(ModelManager.listModels(application))
    val importedModels: StateFlow<List<ModelName>> = _importedModels
    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading: StateFlow<Boolean> = _isBackupLoading

    private val _isRestoreLoading = MutableStateFlow(false)
    val isRestoreLoading: StateFlow<Boolean> = _isRestoreLoading



    companion object {
        private const val PREF_NAME = "AsyncStorage" // used for backward compatibility with old Storage wrapper which has now been removed (I was original as TypeScript guy)
        private const val TAG = "SettingsViewModel"
        const val BACKUP_FILENAME = "smartscan_backup.zip"
        private const val HASH_FILENAME = "hash.txt"
    }

    init {
        _appSettings.value = loadSettings(sharedPrefs)
        ModelManager.listModels(application)
    }

    fun updateIndexFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(indexFrequency = frequency)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun onImportModel( uri: Uri, modelInfo: ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ModelManager.importModel(getApplication(), modelInfo, uri)
                _importedModels.value = ModelManager.listModels(getApplication())
                _event.emit("Model imported successfully")
            } catch (e: Exception) {
                val defaultErrorMessage = "Model import failed"
                val invalidFileError = "Invalid model file"
                Log.e(TAG, "$defaultErrorMessage: ${e.message}")
                val errorMessage = if(e.message == invalidFileError) invalidFileError else defaultErrorMessage
                _event.emit(errorMessage)
            }
        }
    }

    fun onDeleteModel(modelInfo: ModelInfo){
        if(ModelManager.deleteModel(getApplication(), modelInfo)) {
            _importedModels.value = ModelManager.listModels(getApplication())
        }
    }

    fun addSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories + dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories - dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }
    fun addSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories + dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories - dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateTheme(theme: ThemeMode){
        ThemeManager.updateThemeMode(theme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(theme = theme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateColorScheme(colorScheme: ColorSchemeType){
        ThemeManager.updateColorScheme(colorScheme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(color = colorScheme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun backup(uri: Uri){
        val indexZipFile = File(getApplication<Application>().cacheDir, BACKUP_FILENAME)
        val imageIndexFile = File(getApplication<Application>().filesDir, EmbeddingStoresFiles.IMAGE)
        val videoIndexFile = File(getApplication<Application>().filesDir,  EmbeddingStoresFiles.VIDEO)
        val tagsEmbedStoreFile = File(getApplication<Application>().filesDir,  EmbeddingStoresFiles.TAGS)
        val hashFile = File(getApplication<Application>().cacheDir, HASH_FILENAME)
        val imageTagDb = getApplication<Application>().getDatabasePath("image_tag_database")
        val videoTagDb = getApplication<Application>().getDatabasePath("video_tag_database")

        val filesToZip = listOf(imageIndexFile, videoIndexFile, tagsEmbedStoreFile, hashFile, imageTagDb, videoTagDb)
        _isBackupLoading.value = true

        viewModelScope.launch(Dispatchers.IO){
            try {
                if(!imageIndexFile.exists() && !videoIndexFile.exists()) error("Media not indexed")
                val hashes: List<String> = filesToZip.filter { it.exists() && it != hashFile }.map{hashFile(it)}
                hashFile.writeText(hashes.joinToString("\n") )

                zipFiles(indexZipFile, filesToZip)
                copyToUri(getApplication<Application>(), uri, indexZipFile)
                _event.emit("Backup successful")
            }catch (e: Exception){
                Log.e(TAG, "Error backing up: ${e.message}")
                if(e.message == "Media not indexed") _event.emit(e.message!!) else _event.emit("Backup failed")
            }finally {
                indexZipFile.delete()
                hashFile.delete()
                _isBackupLoading.emit(false)
            }
        }
    }

    fun restore(uri: Uri){
        val indexZipFile = File(getApplication<Application>().cacheDir, BACKUP_FILENAME)
        _isRestoreLoading.value = true

        viewModelScope.launch(Dispatchers.IO){
            try {
                copyFromUri(getApplication<Application>(), uri, indexZipFile)
                val extractedFiles = unzipFiles(indexZipFile, getApplication<Application>().filesDir)
                if(!isValidBackupFile((extractedFiles))){
                    extractedFiles.forEach { it.delete() }
                    error("Invalid backup file")
                }
                _event.emit("Restore successful")
                ImageIndexListener.onComplete(getApplication<Application>(), Metrics.Success()) // call onComplete to trigger refresh in search screen
                VideoIndexListener.onComplete(getApplication<Application>(), Metrics.Success())
                sharedPrefs.edit { putString("lastIndexed", System.currentTimeMillis().toString()) } // so scheduling can be triggered
            }catch (e: Exception){
                Log.e(TAG, "Error restoring: ${e.message}")
                if (e.message == "Invalid backup file") _event.emit(e.message!!) else _event.emit("Restore failed")
            }finally {
                indexZipFile.delete()
                _isRestoreLoading.emit(false)
            }
        }
    }

    private suspend fun isValidBackupFile(extractedFiles: List<File>): Boolean{
        val hashFile = extractedFiles.find { it.name == HASH_FILENAME }?: return false
        val hashesFromFile: List<String> = hashFile.readLines()
        if(hashesFromFile.isEmpty()) return false

        val otherFiles = extractedFiles.filterNot{it.name == HASH_FILENAME}
        val otherFileHashes = otherFiles.map{hashFile(it)}
        return hashesFromFile.toSet() == otherFileHashes.toSet()
    }

    fun updateEnableDirectionGalleryOpen(enable: Boolean){
        _appSettings.update{currentSettings -> currentSettings.copy(enableDirectGalleryOpen = enable)}
        saveSettings(sharedPrefs, _appSettings.value)
    }
    fun updateResultsPerRow(n: Int){
        _appSettings.update{currentSettings -> currentSettings.copy(resultsPerRow = n)}
        saveSettings(sharedPrefs, _appSettings.value)
    }
}
