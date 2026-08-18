package com.heydar.filemanager.ui.navigation

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
import com.heydar.filemanager.ui.screens.files.FileListScreen
import com.heydar.filemanager.ui.screens.home.HomeScreen
import com.heydar.filemanager.ui.screens.editor.TextEditorScreen
import com.heydar.filemanager.ui.screens.details.FileDetailsScreen
import com.heydar.filemanager.ui.screens.splash.SplashScreen
import com.heydar.filemanager.ui.screens.splash.SplashUiEvent
import com.heydar.filemanager.ui.screens.splash.SplashViewModel
import com.heydar.filemanager.ui.screens.network.NetworkHomeScreen
import com.heydar.filemanager.ui.screens.network.NetworkShareScreen
import com.heydar.filemanager.ui.screens.network.NetworkBrowserScreen
import com.heydar.filemanager.ui.screens.network.NetworkFileListScreen

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
                    navController.navigate(Screen.FileList.createRoute(encodedPath))
                },
                onNetworkClick = {
                    navController.navigate(Screen.NetworkHome.route)
                }
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
                onBrowseClick = { navController.navigate(Screen.NetworkBrowser.route) }
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