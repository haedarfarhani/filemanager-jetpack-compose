package ir.heydar.nexfolder.ui.screens.home

import ir.heydar.nexfolder.domain.model.StorageVolumeInfo

data class HomeUiState(
    val isLoading: Boolean = true,
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false
)