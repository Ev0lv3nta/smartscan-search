package com.fpf.smartscan


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.ui.components.OverflowMenu
import com.fpf.smartscan.ui.components.UpdatePopUp
import com.fpf.smartscan.ui.screens.collections.CollectionItemsScreen
import com.fpf.smartscan.ui.screens.collections.CollectionsScreen
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.help.HelpScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(intentSearchQuery: SearchQuery?, onAppReady: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainViewModel: MainViewModel = koinViewModel()

    val settingsViewModel: SettingsViewModel = viewModel()
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    val settingsType = navBackStackEntry?.arguments?.getString("type")
    val collectionName = navBackStackEntry?.arguments?.getString("collectionName")


    val headerTitle = when {
        currentRoute == Routes.SEARCH -> stringResource(R.string.title_search)
        currentRoute == Routes.COLLECTIONS -> stringResource(R.string.title_collections)
        currentRoute == Routes.SETTINGS -> stringResource(R.string.title_settings)
        currentRoute == Routes.DONATE -> stringResource(R.string.title_donate)
        currentRoute == Routes.HELP -> stringResource(R.string.title_help)
        currentRoute?.startsWith(Routes.COLLECTION_ITEMS) == true -> collectionName ?: ""
        currentRoute?.startsWith(Routes.SETTINGS.split("/")[0]) == true -> when (settingsType) {
            SettingTypes.THRESHOLD -> stringResource(R.string.setting_similarity_threshold)
            SettingTypes.MODELS -> stringResource(R.string.setting_models)
            SettingTypes.MANAGE_MODELS -> stringResource(R.string.setting_manage_models)
            SettingTypes.SEARCHABLE_IMG_DIRS -> stringResource(R.string.setting_searchable_image_folders)
            SettingTypes.SEARCHABLE_VID_DIRS -> stringResource(R.string.setting_searchable_video_folders)
            SettingTypes.BACKUP_RESTORE -> stringResource(R.string.setting_backup_restore)
            else -> ""
        }

        else -> ""
    }

    val showBackButton = currentRoute?.startsWith(Routes.SETTINGS_DETAIL.split("/")[0]) == true
            || currentRoute?.startsWith(Routes.COLLECTION_ITEMS.split("/")[0]) == true
            || currentRoute in listOf(Routes.DONATE, Routes.HELP)

    val showSearchActions = currentRoute == Routes.SEARCH

    var showImageDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        mainViewModel.prepareApp(){onAppReady() }
    }

    if (showImageDialog || showVideoDialog) {
        val media = if (showImageDialog) "images" else "videos"

        AlertDialog(
            onDismissRequest = {
                if (showImageDialog) showImageDialog = false else showVideoDialog = false
            },
            title = {
                Text(stringResource(R.string.alert_scan_index_title, media))
            },
            text = {
                Column {
                    Text(stringResource(R.string.alert_scan_index_description))

                    Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            if (showImageDialog) {
                                showImageDialog = false
                                mainViewModel.refreshMediaIndex(MediaType.IMAGE)
                            } else {
                                showVideoDialog = false
                                mainViewModel.refreshMediaIndex(MediaType.VIDEO)
                            }
                        }
                    ) {
                        Text("Refresh")
                    }

                    TextButton(
                        onClick = {
                            if (showImageDialog) {
                                showImageDialog = false
                                mainViewModel.rebuildMediaIndex(MediaType.IMAGE)
                            } else {
                                showVideoDialog = false
                                mainViewModel.rebuildMediaIndex(MediaType.VIDEO)
                            }
                        }
                    ) {
                        Text("Rebuild")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showImageDialog) showImageDialog = false else showVideoDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

        if (isUpdatePopUpVisible) {
            UpdatePopUp(
                isVisible = true,
                updates = mainViewModel.getUpdates(),
                onClose = { mainViewModel.closeUpdatePopUp() }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = headerTitle) },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!showSearchActions) return@TopAppBar
                            OverflowMenu(
                                onScanImages = {
                                    showImageDialog=true
                                },
                                onScanVideos = {
                                    showVideoDialog=true
                                },
                            )
                        }
                    )
                },
                bottomBar = { BottomNavigationBar(navController) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.SEARCH,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(Routes.SEARCH) {
                        SearchScreen(
                            appSettings = settingsViewModel.appSettings,
                            intentSearchQuery = intentSearchQuery
                        )
                    }
                    composable(Routes.COLLECTIONS) {
                        CollectionsScreen(
                            onNavigate = { route: String ->
                                navController.navigate(route)
                            }
                        )
                    }
                    composable(
                        route = Routes.COLLECTION_ITEMS,
                        arguments = listOf(
                            navArgument("collectionName") { type = NavType.StringType },
                            navArgument("clusterId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { backStackEntry ->
                        val collectionName = backStackEntry.arguments?.getString("collectionName")
                        val clusterId = backStackEntry.arguments?.getLong("clusterId") ?: -1L
                        CollectionItemsScreen(
                            clusterId = clusterId,
                            collectionName = collectionName,
                            appSettings = settingsViewModel.appSettings,
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigate = { route: String ->
                                navController.navigate(route)
                            }
                        )
                    }
                    composable(
                        route = Routes.SETTINGS_DETAIL,
                        arguments = listOf(navArgument("type") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        SettingsDetailScreen(
                            type = type,
                            viewModel = settingsViewModel,
                        )
                    }
                    composable(Routes.DONATE) {
                        DonateScreen()
                    }

                    composable(Routes.HELP) {
                        HelpScreen()
                    }
                }
            }
        }
    }
