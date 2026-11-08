package ir.heydar.nexfolder.ui.screens.network

data class NetworkFileListUiState(
    val serverUrl: String = "",
    val currentPath: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
