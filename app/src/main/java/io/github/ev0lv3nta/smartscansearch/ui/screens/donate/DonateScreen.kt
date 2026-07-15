package io.github.ev0lv3nta.smartscansearch.ui.screens.donate

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ev0lv3nta.smartscansearch.R
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import io.github.ev0lv3nta.smartscansearch.navigation.TopBarState

@Composable
fun DonateScreen(
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit
    ) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val btcWallet = stringResource(R.string.btc_wallet)
    val ethWallet = stringResource(R.string.eth_wallet)
    val ltcWallet = stringResource(R.string.ltc_wallet)
    val koFiUrl = stringResource(R.string.donate_kofi_url)

    val screenTitle = stringResource(R.string.title_donate)


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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = stringResource(R.string.donate_support_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, koFiUrl.toUri())
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Text(text = "Support on Ko-fi")
        }

        Text(
            text = "Donate with crypto",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            CryptoWalletOptionRow(
                coinName = stringResource(R.string.label_btc),
                walletAddress = btcWallet,
                onCopyClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText("btc wallet", AnnotatedString(btcWallet))
                            )
                        )
                    }
                }
            )

            CryptoWalletOptionRow(
                coinName = stringResource(R.string.label_eth),
                walletAddress = ethWallet,
                onCopyClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText("eth wallet", AnnotatedString(ethWallet))
                            )
                        )
                    }
                }
            )

            CryptoWalletOptionRow(
                coinName = stringResource(R.string.label_ltc),
                walletAddress = ltcWallet,
                onCopyClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText("ltc wallet", AnnotatedString(ltcWallet))
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun CryptoWalletOptionRow(
    coinName: String,
    walletAddress: String,
    onCopyClick: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = coinName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = walletAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.alpha(0.9f)
            )
        }

        IconButton(onClick = onCopyClick) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy wallet address",
            )
        }
    }
}