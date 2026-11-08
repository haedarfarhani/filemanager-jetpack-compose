package ir.heydar.nexfolder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ir.heydar.nexfolder.core.settings.ThemeMode

/**
 * "Red Sunburst" brand theme — Night Bordeaux, Dark Amaranth, Amaranth,
 * Coral Glow and Sandy Brown mapped onto Material 3 color roles.
 */
private val LightColorScheme = lightColorScheme(
    primary = DarkAmaranth,
    onPrimary = Color.White,
    primaryContainer = SunburstPrimaryContainerLight,
    onPrimaryContainer = NightBordeaux,
    secondary = Amaranth,
    onSecondary = Color.White,
    secondaryContainer = SunburstSecondaryContainerLight,
    onSecondaryContainer = NightBordeaux,
    tertiary = Color(0xFF8F4A00),
    onTertiary = Color.White,
    tertiaryContainer = SunburstTertiaryContainerLight,
    onTertiaryContainer = Color(0xFF331300),
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = SunburstBackgroundLight,
    onBackground = Color(0xFF221A1A),
    surface = SunburstBackgroundLight,
    onSurface = Color(0xFF221A1A),
    surfaceVariant = SunburstSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF534344),
    outline = Color(0xFF857374),
    outlineVariant = Color(0xFFD9C2C1),
    scrim = Color.Black,
    inverseSurface = Color(0xFF382E2E),
    inverseOnSurface = Color(0xFFFFEDEC),
    inversePrimary = CoralGlow,
    surfaceTint = DarkAmaranth
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralGlow,
    onPrimary = NightBordeaux,
    primaryContainer = DarkAmaranth,
    onPrimaryContainer = SunburstPrimaryContainerLight,
    secondary = Color(0xFFFFB3B9),
    onSecondary = Color(0xFF55131F),
    secondaryContainer = Color(0xFF713039),
    onSecondaryContainer = SunburstSecondaryContainerLight,
    tertiary = SandyBrown,
    onTertiary = Color(0xFF4E2500),
    tertiaryContainer = Color(0xFF6E3800),
    onTertiaryContainer = SunburstTertiaryContainerLight,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = SunburstBackgroundDark,
    onBackground = Color(0xFFEDE0DF),
    surface = SunburstBackgroundDark,
    onSurface = Color(0xFFEDE0DF),
    surfaceVariant = SunburstSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD9C2C1),
    outline = Color(0xFFA08C8C),
    outlineVariant = SunburstSurfaceVariantDark,
    scrim = Color.Black,
    inverseSurface = Color(0xFFEDE0DF),
    inverseOnSurface = Color(0xFF382E2E),
    inversePrimary = DarkAmaranth,
    surfaceTint = CoralGlow
)

@Composable
fun FileManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
