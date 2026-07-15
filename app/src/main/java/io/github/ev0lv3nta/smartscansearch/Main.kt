package io.github.ev0lv3nta.smartscansearch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.ev0lv3nta.smartscansearch.index.IndexingStatus
import io.github.ev0lv3nta.smartscansearch.media.MediaCollection
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import io.github.ev0lv3nta.smartscansearch.navigation.Routes
import io.github.ev0lv3nta.smartscansearch.navigation.BottomNavigationBar
import io.github.ev0lv3nta.smartscansearch.navigation.NavDataKeys
import io.github.ev0lv3nta.smartscansearch.navigation.TopBarState
import io.github.ev0lv3nta.smartscansearch.search.SearchQuery
import io.github.ev0lv3nta.smartscansearch.ui.components.ScanLoadingView
import io.github.ev0lv3nta.smartscansearch.ui.components.ScanModal
import io.github.ev0lv3nta.smartscansearch.ui.components.UpdatePopUp
import io.github.ev0lv3nta.smartscansearch.ui.permissions.RequestPermissions
import io.github.ev0lv3nta.smartscansearch.ui.permissions.StorageAccess
import io.github.ev0lv3nta.smartscansearch.ui.permissions.getStorageAccess
import io.github.ev0lv3nta.smartscansearch.ui.screens.collections.CollectionItemsScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.collections.CollectionsScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.donate.DonateScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.search.SearchScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.settings.SettingsDetailScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.settings.SettingsScreen
import io.github.ev0lv3nta.smartscansearch.ui.screens.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(
    intentSearchQuery: SearchQuery?,
    onAppReady: () -> Unit,
    onRestartApp: () -> Unit
) {
    val context = LocalContext.current
    val topBarState = remember { mutableStateOf(TopBarState()) }
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val mainViewModel: MainViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    val imageIndexProgress by mainViewModel.imageIndexProgress.collectAsState()
    val videoIndexProgress by mainViewModel.videoIndexProgress.collectAsState()
    val imageIndexStatus by mainViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by mainViewModel.videoIndexStatus.collectAsState()
    val hasIndexedImages by mainViewModel.hasIndexedImages.collectAsState()
    val hasIndexedVideos by mainViewModel.hasIndexedVideos.collectAsState()
    val runningMediaTypes by mainViewModel.runningMediaTypes.collectAsState()
    val isIndexing = imageIndexStatus == IndexingStatus.ACTIVE || videoIndexStatus == IndexingStatus.ACTIVE || runningMediaTypes.isNotEmpty()

    var hasStoragePermission by remember { mutableStateOf(false) }
    var showFirstScanModal by remember { mutableStateOf(false) }
    var showScanAndRebuildModal by remember { mutableStateOf(false) }
    var showRefreshScanModel by remember { mutableStateOf(false) }
    var requiredMediaTypeToIndex by remember { mutableStateOf<MediaType?>(null) }

    LaunchedEffect(Unit) {
        hasStoragePermission = getStorageAccess(context) != StorageAccess.Denied
        mainViewModel.prepareApp { onAppReady() }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus in listOf(IndexingStatus.COMPLETE, IndexingStatus.FAILED)) {
            mainViewModel.onIndexingFinished(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus in listOf(IndexingStatus.COMPLETE, IndexingStatus.FAILED)) {
            mainViewModel.onIndexingFinished(MediaType.VIDEO)
        }
    }

    if (currentRoute in listOf(Routes.SEARCH, Routes.COLLECTIONS)) {
        RequestPermissions { _, storageGranted -> hasStoragePermission = storageGranted }
    }

    if (isUpdatePopUpVisible) {
        UpdatePopUp(
            isVisible = true,
            updates = mainViewModel.getUpdates(),
            onClose = { mainViewModel.closeUpdatePopUp() },
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(topBarState.value.title)
                    },
                    navigationIcon = {
                        topBarState.value.navigationIcon?.invoke()
                    },
                    actions = {
                        topBarState.value.actions?.invoke(this)
                    }
                )
            },
            bottomBar = { if (!isIndexing) BottomNavigationBar(navController) }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                if(isIndexing) {
                    ScanLoadingView(
                        isIndexing = true,
                        imageIndexStatus = imageIndexStatus,
                        videoIndexStatus = videoIndexStatus,
                        videoIndexProgress = videoIndexProgress,
                        imageIndexProgress = imageIndexProgress,
                        title = stringResource(R.string.scan_in_progress_title),
                        message = stringResource(R.string.scan_in_progress_content)
                    )
                }else {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.SEARCH,
                    ) {
                        composable(Routes.SEARCH) {
                            SearchScreen(
                                appSettings = settingsViewModel.appSettings,
                                onTopBarChange = { topBarState.value = it },
                                intentSearchQuery = intentSearchQuery,
                                isIndexing = isIndexing,
                                hasIndexedImages = hasIndexedImages,
                                hasIndexedVideos = hasIndexedVideos,
                                hasStoragePermission = hasStoragePermission,
                                onIndex = {
                                    requiredMediaTypeToIndex = it
                                    showFirstScanModal = true
                                },
                                )
                        }
                        composable(Routes.COLLECTIONS) {
                            CollectionsScreen(
                                isIndexing = isIndexing,
                                hasIndexedImages = hasIndexedImages,
                                hasIndexedVideos = hasIndexedVideos,
                                hasStoragePermission = hasStoragePermission,
                                onIndex = { showFirstScanModal = true },
                                onTopBarChange = { topBarState.value = it },
                                onViewCollection = { collection ->
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(NavDataKeys.COLLECTION, collection)

                                    navController.navigate(Routes.COLLECTION_ITEMS)
                                },
                            )
                        }
                        composable(
                            route = Routes.COLLECTION_ITEMS,
                        ) { _ ->
                            val collection =
                                navController.previousBackStackEntry?.savedStateHandle?.get<MediaCollection>(
                                    NavDataKeys.COLLECTION
                                )

                            CollectionItemsScreen(
                                onTopBarChange = { topBarState.value = it },
                                collection = collection,
                                appSettings = settingsViewModel.appSettings,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onTopBarChange = { topBarState.value = it },
                                viewModel = settingsViewModel,
                                onNavigate = { route: String ->
                                    navController.navigate(route)
                                },
                                onRestartApp = onRestartApp,
                                onScanRebuild = { showScanAndRebuildModal = true },
                                onScanRefresh = { showRefreshScanModel = true }
                            )
                        }
                        composable(
                            route = Routes.SETTINGS_DETAIL,
                            arguments = listOf(navArgument("type") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: ""
                            SettingsDetailScreen(
                                onTopBarChange = { topBarState.value = it },
                                type = type,
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.DONATE) {
                            DonateScreen(
                                onTopBarChange = { topBarState.value = it },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    ScanModal(
        showFirstScanModal,
        onClose = {
            showFirstScanModal = false
            requiredMediaTypeToIndex = null
        },
        onConfirm = {
            mainViewModel.refreshMediaIndex(it)
            showFirstScanModal = false
            requiredMediaTypeToIndex = null
        },
        title = requiredMediaTypeToIndex?.let {
            stringResource(
                R.string.scan_media_action,
                it.name.lowercase() + "s"
            )
        } ?: stringResource(R.string.scan_action),
        description = requiredMediaTypeToIndex?.let {
            stringResource(
                R.string.alert_first_media_scan_content,
                it.name.lowercase()
            )
        } ?: stringResource(R.string.alert_first_scan_content),
        mediaType = requiredMediaTypeToIndex
    )

    ScanModal(
        showRefreshScanModel,
        onClose = {
            showRefreshScanModel = false
        },
        onConfirm = {
            mainViewModel.refreshMediaIndex(it)
            showRefreshScanModel = false
        },
        title = stringResource(R.string.scan_action),
        description = stringResource(R.string.alert_scan_refresh_description),
    )

    ScanModal(
        showScanAndRebuildModal,
        onClose = {
            showScanAndRebuildModal = false
        },
        onConfirm = {
            mainViewModel.rebuildMediaIndex(it)
            showScanAndRebuildModal = false
        },
        title = stringResource(R.string.scan_rebuild_action),
        description = stringResource(R.string.alert_scan_rebuild_description),
    )
}
