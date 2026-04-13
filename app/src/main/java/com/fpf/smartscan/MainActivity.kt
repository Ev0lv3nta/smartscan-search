package com.fpf.smartscan

import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.ui.theme.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sharedPrefs by lazy { application.getSharedPreferences("AsyncStorage", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appSettings = loadSettings(sharedPrefs)
        ThemeManager.updateColorScheme(appSettings.color)
        ThemeManager.updateThemeMode(appSettings.theme)

        createNotificationChannel(
            channelId = getString(R.string.worker_channel_id),
            channelName = getString(R.string.worker_channel_name),
            description = getString(R.string.worker_channel_description)
        )
        updateEdgeToEdge()

        lifecycleScope.launch {
            ThemeManager.themeMode.collectLatest {
                updateEdgeToEdge()
            }
        }

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this).components {
                add(VideoFrameDecoder.Factory())
            }
                .crossfade(true)
                .memoryCache { MemoryCache.Builder().maxSizePercent(this, 0.25).build() }
                .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizePercent(0.05).build() }
                .build()
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
            App {
                MainScreen(intentSearchQuery)
            }
        }
    }

    override fun onResume(){
        super.onResume()
    }

    private fun createNotificationChannel(channelId: String, channelName: String, description: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply { this.description = description }
        notificationManager.createNotificationChannel(channel)
    }
    private fun updateEdgeToEdge() {
        val isDarkTheme = ThemeManager.isDarkTheme(resources)
        enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb()),
            navigationBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb())
        )
    }
}
