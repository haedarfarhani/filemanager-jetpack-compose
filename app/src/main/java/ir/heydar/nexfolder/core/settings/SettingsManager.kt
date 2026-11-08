package ir.heydar.nexfolder.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** App-wide theme preference. */
enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark")
}

/** Persisted, observable app settings. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Use Material You wallpaper colors (Android 12+) instead of the Red Sunburst brand. */
    val dynamicColor: Boolean = false,
    /** Show dot-prefixed hidden files in listings and search. */
    val showHiddenFiles: Boolean = false,
    /** Ask before deleting files and folders. */
    val confirmDelete: Boolean = true
)

/** SharedPreferences-backed settings store with a StateFlow for reactive UI. */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) }
            ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, false),
        showHiddenFiles = prefs.getBoolean(KEY_HIDDEN, false),
        confirmDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true)
    )

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _settings.update { it.copy(dynamicColor = enabled) }
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDDEN, enabled).apply()
        _settings.update { it.copy(showHiddenFiles = enabled) }
    }

    fun setConfirmDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
        _settings.update { it.copy(confirmDelete = enabled) }
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_HIDDEN = "show_hidden_files"
        const val KEY_CONFIRM_DELETE = "confirm_delete"
    }
}
