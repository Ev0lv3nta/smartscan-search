package com.fpf.smartscan.ui.screens.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.navigation.TopBarState

@Composable
fun HelpScreen(
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val screenTitle = stringResource(R.string.title_help)

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
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Text(
                text = stringResource(R.string.help_search_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.help_media_search_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.help_indexing_first_time))
            BulletPoint(stringResource(R.string.help_indexing_schedule))
            BulletPoint(stringResource(R.string.help_manual_refresh_index))

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.help_search_tips_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.help_short_specific_queries))
            BulletPoint(stringResource(R.string.help_include_text_from_media))
            BulletPoint(stringResource(R.string.help_reverse_image_search))
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = "\u2022", // Bullet character
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
