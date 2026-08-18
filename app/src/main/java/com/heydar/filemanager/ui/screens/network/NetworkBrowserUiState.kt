package com.heydar.filemanager.ui.screens.network

import com.heydar.filemanager.domain.model.network.NetworkShareInfo

data class NetworkBrowserUiState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<NetworkShareInfo> = emptyList(),
    val error: String? = null,
    val showManualConnect: Boolean = false,
    val manualIp: String = "",
    val manualPort: String = "8080"
)
