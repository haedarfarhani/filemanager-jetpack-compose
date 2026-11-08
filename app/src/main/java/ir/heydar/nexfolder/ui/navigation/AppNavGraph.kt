package ir.heydar.nexfolder.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ir.heydar.nexfolder.ui.screens.files.FileListScreen
import ir.heydar.nexfolder.ui.screens.home.HomeScreen
import ir.heydar.nexfolder.ui.screens.editor.TextEditorScreen
import ir.heydar.nexfolder.ui.screens.details.FileDetailsScreen
import ir.heydar.nexfolder.ui.screens.search.SearchScreen
import ir.heydar.nexfolder.ui.screens.analyzer.StorageAnalyzerScreen
import ir.heydar.nexfolder.ui.screens.recents.RecentsScreen
import ir.heydar.nexfolder.ui.screens.viewer.ImageViewerScreen
import ir.heydar.nexfolder.ui.screens.player.AudioPlayerScreen
import ir.heydar.nexfolder.ui.screens.archive.ArchiveBrowserScreen
import ir.heydar.nexfolder.ui.screens.splash.SplashScreen
import ir.heydar.nexfolder.ui.screens.splash.SplashUiEvent
import ir.heydar.nexfolder.ui.screens.splash.SplashViewModel
import ir.heydar.nexfolder.ui.screens.network.NetworkHomeScreen
import ir.heydar.nexfolder.ui.screens.network.NetworkShareScreen
import ir.heydar.nexfolder.ui.screens.network.NetworkBrowserScreen
import ir.heydar.nexfolder.ui.screens.network.NetworkFileListScreen
import ir.heydar.nexfolder.ui.screens.network.ConnectionsScreen
import ir.heydar.nexfolder.ui.screens.network.RemoteBrowseScreen
import ir.heydar.nexfolder.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val viewModel: SplashViewModel = hiltViewModel()
            val uiEvent by viewModel.uiEvent.collectAsState(initial = null)

            LaunchedEffect(uiEvent) {
                when (uiEvent) {
                    is SplashUiEvent.NavigateToHome -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                    null -> Unit
                }
            }

            SplashScreen()
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onStorageClick = { encodedPath ->
                    navController.navigate(Screen.FileList.createRoute(encodedPath)) {
                        launchSingleTop = true
                    }
                },
                onNetworkClick = {
                    navController.navigate(Screen.NetworkHome.route) {
                        launchSingleTop = true
                    }
                },
                onAnalyzeClick = { encodedPath ->
                    navController.navigate(Screen.StorageAnalyzer.createRoute(encodedPath)) {
                        launchSingleTop = true
                    }
                },
                onSearchClick = { scopePath ->
                    navController.navigate(Screen.Search.createRoute(scopePath)) {
                        launchSingleTop = true
                    }
                },
                onRecentsClick = {
                    navController.navigate(Screen.Recents.route) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StorageAnalyzer.route,
            arguments = listOf(
                navArgument("rootPath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("rootPath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            StorageAnalyzerScreen(
                rootPath = decodedPath,
                onNavigateBack = { navController.popBackStack() },
                onOpenDetails = { filePath ->
                    navController.navigate(Screen.FileDetails.createRoute(Uri.encode(filePath)))
                },
                onOpenFolder = { folderPath ->
                    navController.navigate(Screen.FileList.createRoute(Uri.encode(folderPath)))
                }
            )
        }

        composable(Screen.Recents.route) {
            RecentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenTextEditor = { filePath ->
                    navController.navigate(Screen.TextEditor.createRoute(Uri.encode(filePath)))
                },
                onOpenDetails = { filePath ->
                    navController.navigate(Screen.FileDetails.createRoute(Uri.encode(filePath)))
                },
                onOpenViewer = { filePath ->
                    navController.navigate(Screen.ImageViewer.createRoute(Uri.encode(filePath)))
                },
                onOpenPlayer = { filePath ->
                    navController.navigate(Screen.AudioPlayer.createRoute(Uri.encode(filePath)))
                }
            )
        }

        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            ImageViewerScreen(
                filePath = decodedPath,
                onNavigateBack = { navController.popBackStack() },
                onOpenDetails = { p ->
                    navController.navigate(Screen.FileDetails.createRoute(Uri.encode(p)))
                }
            )
        }

        composable(
            route = Screen.AudioPlayer.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            AudioPlayerScreen(
                filePath = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Archive.route,
            arguments = listOf(
                navArgument("archivePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("archivePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            ArchiveBrowserScreen(
                archivePath = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FileList.route,
            arguments = listOf(
                navArgument("rootPath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("rootPath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            FileListScreen(
                rootPath = decodedPath,
                onNavigateUp = { navController.popBackStack() },
                onOpenTextEditor = { filePath ->
                    navController.navigate(Screen.TextEditor.createRoute(Uri.encode(filePath)))
                },
                onOpenDetails = { filePath ->
                    navController.navigate(Screen.FileDetails.createRoute(Uri.encode(filePath)))
                },
                onOpenSearch = { scopePath ->
                    navController.navigate(Screen.Search.createRoute(scopePath))
                },
                onOpenViewer = { filePath ->
                    navController.navigate(Screen.ImageViewer.createRoute(Uri.encode(filePath)))
                },
                onOpenPlayer = { filePath ->
                    navController.navigate(Screen.AudioPlayer.createRoute(Uri.encode(filePath)))
                },
                onOpenArchive = { archivePath ->
                    navController.navigate(Screen.Archive.createRoute(archivePath))
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("scopePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("scopePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            SearchScreen(
                scopePath = decodedPath,
                onNavigateBack = { navController.popBackStack() },
                onOpenTextEditor = { filePath ->
                    navController.navigate(Screen.TextEditor.createRoute(Uri.encode(filePath)))
                },
                onOpenDetails = { filePath ->
                    navController.navigate(Screen.FileDetails.createRoute(Uri.encode(filePath)))
                }
            )
        }

        composable(
            route = Screen.TextEditor.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            TextEditorScreen(
                filePath = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.FileDetails.route,
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            FileDetailsScreen(
                filePath = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Network screens
        composable(Screen.NetworkHome.route) {
            NetworkHomeScreen(
                onNavigateBack = { navController.popBackStack() },
                onShareClick = { navController.navigate(Screen.NetworkShare.route) },
                onBrowseClick = { navController.navigate(Screen.NetworkBrowser.route) },
                onConnectionsClick = { navController.navigate(Screen.Connections.route) }
            )
        }

        composable(Screen.Connections.route) {
            ConnectionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onConnect = { connectionId ->
                    navController.navigate(Screen.RemoteBrowse.createRoute(connectionId))
                }
            )
        }

        composable(
            route = Screen.RemoteBrowse.route,
            arguments = listOf(
                navArgument("connectionId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("connectionId") ?: ""
            RemoteBrowseScreen(
                connectionId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkShare.route) {
            NetworkShareScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkBrowser.route) {
            NetworkBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceClick = { serverUrl ->
                    navController.navigate(Screen.NetworkFileList.createRoute(serverUrl))
                }
            )
        }

        composable(
            route = Screen.NetworkFileList.route,
            arguments = listOf(
                navArgument("serverUrl") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
            val decodedUrl = Uri.decode(encodedUrl)
            NetworkFileListScreen(
                serverUrl = decodedUrl,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}