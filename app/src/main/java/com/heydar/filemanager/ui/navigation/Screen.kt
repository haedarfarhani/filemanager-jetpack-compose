package com.heydar.filemanager.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object FileList : Screen("fileList/{rootPath}") {
        fun createRoute(encodedPath: String) = "fileList/$encodedPath"
    }
    data object TextEditor : Screen("textEditor/{filePath}") {
        fun createRoute(encodedFilePath: String) = "textEditor/$encodedFilePath"
    }
    data object FileDetails : Screen("fileDetails/{filePath}") {
        fun createRoute(encodedFilePath: String) = "fileDetails/$encodedFilePath"
    }
    data object NetworkHome : Screen("networkHome")
    data object NetworkShare : Screen("networkShare")
    data object NetworkBrowser : Screen("networkBrowser")
    data object NetworkFileList : Screen("networkFileList/{serverUrl}") {
        fun createRoute(serverUrl: String) = "networkFileList/${android.net.Uri.encode(serverUrl)}"
    }
}
