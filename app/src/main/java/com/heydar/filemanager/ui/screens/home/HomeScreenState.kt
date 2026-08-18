package com.heydar.filemanager.ui.screens.home

import com.heydar.filemanager.domain.model.StorageVolumeInfo

data class HomeUiState(
    val isLoading: Boolean = true,
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false
)