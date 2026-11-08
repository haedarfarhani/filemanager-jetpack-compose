package ir.heydar.nexfolder.ui.screens.home

import android.app.Application
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.nexfolder.domain.model.StorageVolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadStorageVolumes -> loadVolumes()
            is HomeEvent.OnStorageClicked -> { /* Handled by UI via callback */ }
            is HomeEvent.ShowError -> {
                _state.update { it.copy(isLoading = false, errorMessage = event.message) }
            }
            is HomeEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun loadVolumes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val volumes = withContext(Dispatchers.IO) {
                    buildStorageVolumeList()
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        storageVolumes = volumes,
                        permissionGranted = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load storage information"
                    )
                }
            }
        }
    }

    private fun buildStorageVolumeList(): List<StorageVolumeInfo> {
        val storageManager = app.getSystemService(StorageManager::class.java)
        val rawVolumes = storageManager.storageVolumes

        return rawVolumes.mapNotNull { volume ->
            try {
                val path = getVolumePath(volume) ?: return@mapNotNull null
                val dir = File(path)
                if (!dir.exists()) return@mapNotNull null

                val total = dir.totalSpace
                val free = dir.freeSpace
                val used = total - free

                StorageVolumeInfo(
                    id = volume.uuid ?: "primary",
                    name = volume.getDescription(app) ?: "Storage",
                    path = path,
                    totalBytes = total,
                    freeBytes = free,
                    usedBytes = used,
                    isPrimary = volume.isPrimary,
                    isRemovable = volume.isRemovable,
                    isUsb = ir.heydar.nexfolder.core.storage.UsbStorageDetector.isUsbVolume(app, volume, path)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getVolumePath(volume: android.os.storage.StorageVolume): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volume.directory?.absolutePath
        } else {
            @Suppress("DEPRECATION")
            if (volume.isPrimary) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                volume.uuid?.let { "/storage/$it" }
            }
        }
    }
}