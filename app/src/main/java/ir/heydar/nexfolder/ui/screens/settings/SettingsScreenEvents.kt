package ir.heydar.nexfolder.ui.screens.settings

import ir.heydar.nexfolder.core.settings.ThemeMode

sealed class SettingsEvent {
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsEvent()
    data class DynamicColorChanged(val enabled: Boolean) : SettingsEvent()
    data class ShowHiddenChanged(val enabled: Boolean) : SettingsEvent()
    data class ConfirmDeleteChanged(val enabled: Boolean) : SettingsEvent()
}
