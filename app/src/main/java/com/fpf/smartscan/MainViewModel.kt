package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.EmbedStoreSyncHelper
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore
) : AndroidViewModel(application) {

    companion object {
    }
    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)
    private val hasSyncedDates by lazy { sharedPrefs.getBoolean(PrefsKeys.SYNC_COMPLETE, false)}

    private val _appReady = MutableStateFlow(false)
    val appReady: StateFlow<Boolean> = _appReady

    val versionName: String? = try {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        null
    }

    private val _isUpdatePopUpVisible = MutableStateFlow(!hasShownUpdatePopUp)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val hasShownUpdatePopUp: Boolean
        get() = sharedPrefs.getString(PrefsKeys.UPDATES, null) == versionName

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        sharedPrefs.edit { putString(PrefsKeys.UPDATES, versionName.toString()) }
    }

    fun getUpdates(): List<String>{
        return listOf(
            application.getString(R.string.update_cluster_search),
            application.getString(R.string.update_collections),
            application.getString(R.string.update_merge_collections),
            application.getString(R.string.update_copy_to_tag_collections),
            application.getString(R.string.update_swipe_gestures_media_viewer),
            application.getString(R.string.update_results_per_row_setting),
            application.getString(R.string.update_similarity_threshold_setting),
            application.getString(R.string.update_donate_kofi)
        )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!hasSyncedDates) {
                EmbedStoreSyncHelper.syncStores(getApplication(), imageStore, videoStore)
            }
            onAppReady()
        }
    }

}