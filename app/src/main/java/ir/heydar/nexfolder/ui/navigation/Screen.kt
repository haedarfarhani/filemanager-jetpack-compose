package ir.heydar.nexfolder.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object FileList : Screen("fileList/{rootPath}") {
        fun createRoute(encodedPath: String) = "fileList/$encodedPath"
    }
    data object TextEditor : Screen("textEditor/{filePath}") {
        fun createRoute(encodedFilePath: String) = "textEditor/$encodedFilePath"
    }
    data object FileDetails : Screen("fileDetails/{filePath}") {
        fun createRoute(encodedFilePath: String) = "fileDetails/$encodedFilePath"
    }
    data object Search : Screen("search/{scopePath}") {
        fun createRoute(scopePath: String) = "search/${android.net.Uri.encode(scopePath)}"
    }
    data object StorageAnalyzer : Screen("analyzer/{rootPath}") {
        fun createRoute(rootPath: String) = "analyzer/${android.net.Uri.encode(rootPath)}"
    }
    data object Recents : Screen("recents")
    data object ImageViewer : Screen("imageViewer/{filePath}") {
        fun createRoute(encodedFilePath: String) = "imageViewer/$encodedFilePath"
    }
    data object AudioPlayer : Screen("audioPlayer/{filePath}") {
        fun createRoute(encodedFilePath: String) = "audioPlayer/$encodedFilePath"
    }
    data object Archive : Screen("archive/{archivePath}") {
        fun createRoute(archivePath: String) = "archive/${android.net.Uri.encode(archivePath)}"
    }
    data object Connections : Screen("connections")
    data object RemoteBrowse : Screen("remoteBrowse/{connectionId}") {
        fun createRoute(connectionId: String) = "remoteBrowse/$connectionId"
    }
    data object NetworkHome : Screen("networkHome")
    data object NetworkShare : Screen("networkShare")
    data object NetworkBrowser : Screen("networkBrowser")
    data object NetworkFileList : Screen("networkFileList/{serverUrl}") {
        fun createRoute(serverUrl: String) = "networkFileList/${android.net.Uri.encode(serverUrl)}"
    }
}
