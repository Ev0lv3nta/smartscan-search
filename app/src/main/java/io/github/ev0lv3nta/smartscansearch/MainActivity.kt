package io.github.ev0lv3nta.smartscansearch

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.github.ev0lv3nta.smartscansearch.constants.PrefsNames
import io.github.ev0lv3nta.smartscansearch.data.DataSyncHelper
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import io.github.ev0lv3nta.smartscansearch.search.SearchQuery
import io.github.ev0lv3nta.smartscansearch.settings.loadSettings
import io.github.ev0lv3nta.smartscansearch.ui.theme.ThemeManager
import io.github.ev0lv3nta.smartscansearch.utils.BackupUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }


        val appSettings = loadSettings(sharedPrefs)
        ThemeManager.updateColorScheme(appSettings.color)
        ThemeManager.updateThemeMode(appSettings.theme)

        updateEdgeToEdge()

        lifecycleScope.launch {
            ThemeManager.themeMode.collectLatest {
                updateEdgeToEdge()
            }
        }
        var intentSearchQuery: SearchQuery? = null

        val mediaType = intent.getStringExtra("media_type")?.let { MediaType.valueOf(it) } ?: MediaType.IMAGE

        if (intent?.action == Intent.ACTION_SEND && intent?.type?.startsWith("image/") == true) {
           if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
               (intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))?.let {
                   intentSearchQuery = SearchQuery.ImageQuery(uri = it, mediaType=mediaType)
               }
           }else{
               (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM))?.let {
                   if(it is Uri) intentSearchQuery = SearchQuery.ImageQuery(uri = it, mediaType=mediaType)
               }
           }

        } else if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    intentSearchQuery = SearchQuery.TextQuery(text = it, mediaType=mediaType)
                }
        }

        setContent {
            AppTheme {
                Main(
                    intentSearchQuery=intentSearchQuery,
                    onAppReady = {keepSplash = false},
                    onRestartApp = {restartApp()}
                )
            }
        }
    }

    override fun onResume(){
        super.onResume()
    }
    private fun updateEdgeToEdge() {
        val isDarkTheme = ThemeManager.isDarkTheme(resources)
        enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb()),
            navigationBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb())
        )
    }

    fun restartApp() {
        val cachedDb = BackupUtils.checkCachedDb(application)
        val isRestoreRequired = cachedDb != null
        if (isRestoreRequired) {
            BackupUtils.restoreDbFromCache(application, cachedDb)
        }

        App.resetKoin(application)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        startActivity(intent)
    }
}
