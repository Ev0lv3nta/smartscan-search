package io.github.ev0lv3nta.smartscansearch.settings

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }

fun loadSettings(sharedPrefs: SharedPreferences): AppSettings {
    val jsonSettings = sharedPrefs.getString("app_settings", null)
    return if (jsonSettings != null) {
        try {
            json.decodeFromString<AppSettings>(jsonSettings)
        } catch (e: Exception) {
            Log.e("loadSettings", "Failed to decode settings", e)
            AppSettings()
        }
    } else {
        AppSettings()
    }
}

fun saveSettings(sharedPrefs: SharedPreferences, settings: AppSettings) {
    sharedPrefs.edit {putString("app_settings", json.encodeToString(settings))  }
}