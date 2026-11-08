package ir.heydar.nexfolder.ui.screens.settings

import androidx.lifecycle.ViewModel
import ir.heydar.nexfolder.core.settings.AppSettings
import ir.heydar.nexfolder.core.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsManager.settings

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeModeChanged -> settingsManager.setThemeMode(event.mode)
            is SettingsEvent.DynamicColorChanged -> settingsManager.setDynamicColor(event.enabled)
            is SettingsEvent.ShowHiddenChanged -> settingsManager.setShowHiddenFiles(event.enabled)
            is SettingsEvent.ConfirmDeleteChanged -> settingsManager.setConfirmDelete(event.enabled)
        }
    }
}
