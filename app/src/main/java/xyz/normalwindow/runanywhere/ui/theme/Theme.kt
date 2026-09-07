package xyz.normalwindow.runanywhere.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import xyz.normalwindow.runanywhere.data.settings.AppThemeColor
import xyz.normalwindow.runanywhere.data.settings.AppThemeMode
import xyz.normalwindow.runanywhere.data.settings.SettingsRepository

private val LightOrangeColorScheme = lightColorScheme(
    primary = Primary60,
    // Ink, not a near-white: see [OnBrandInk]. Every M3 filled Button, the composer's send
    // action and the selected filter chips take their foreground from this one token.
    onPrimary = OnBrandInk,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary20,
    secondary = Secondary40,
    onSecondary = Neutral99,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Neutral99,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    error = Error40,
    onError = Neutral99,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
    surfaceTint = Primary60,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Primary80,
    scrim = Neutral10,
)
private val DarkOrangeColorScheme = darkColorScheme(
    primary = BrandOrange,
    // Ink, not white: see [OnBrandInk].
    onPrimary = OnBrandInk,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    surfaceTint = Primary70,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Primary60,
    scrim = Neutral10,
)

private val LightTealColorScheme = lightColorScheme(
    primary = Teal60, onPrimary = Neutral100, primaryContainer = Teal90, onPrimaryContainer = Teal20,
    secondary = Teal60, onSecondary = Neutral100, secondaryContainer = Teal90, onSecondaryContainer = Teal20,
    tertiary = Tertiary40, onTertiary = Neutral99, tertiaryContainer = Tertiary90, onTertiaryContainer = Tertiary10,
    error = Error40, onError = Neutral99, errorContainer = Error90, onErrorContainer = Error10,
    background = Neutral98, onBackground = Neutral10, surface = Neutral98, onSurface = Neutral10,
    surfaceVariant = NeutralVariant90, onSurfaceVariant = NeutralVariant30,
    surfaceContainerLowest = Neutral100, surfaceContainerLow = Neutral96, surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92, surfaceContainerHighest = Neutral90, surfaceTint = Teal60,
    outline = NeutralVariant50, outlineVariant = NeutralVariant80, inverseSurface = Neutral20,
    inverseOnSurface = Neutral95, inversePrimary = Teal80, scrim = Neutral10,
)

private val DarkTealColorScheme = darkColorScheme(
    primary = Teal70, onPrimary = Teal20, primaryContainer = Teal30, onPrimaryContainer = Teal90,
    secondary = Teal80, onSecondary = Teal20, secondaryContainer = Teal30, onSecondaryContainer = Teal90,
    tertiary = Tertiary80, onTertiary = Tertiary20, tertiaryContainer = Tertiary30, onTertiaryContainer = Tertiary90,
    error = Error80, onError = Error20, errorContainer = Error30, onErrorContainer = Error90,
    background = Neutral6, onBackground = Neutral90, surface = Neutral6, onSurface = Neutral90,
    surfaceVariant = NeutralVariant30, onSurfaceVariant = NeutralVariant80,
    surfaceContainerLowest = Neutral4, surfaceContainerLow = Neutral10, surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17, surfaceContainerHighest = Neutral22, surfaceTint = Teal70,
    outline = NeutralVariant60, outlineVariant = NeutralVariant30, inverseSurface = Neutral90,
    inverseOnSurface = Neutral20, inversePrimary = Teal60, scrim = Neutral10,
)

@Composable
fun RunAnywhereAITheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val settings = SettingsRepository.settings
    val resolvedDark = darkTheme ?: when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val teal = settings.themeColor == AppThemeColor.TEAL
    CompositionLocalProvider(
        LocalDimens provides CompactDimens,
        // Read once at the theme root so every animated surface can honour
        // reduce-motion (DESIGN_GUIDELINE §6.5) without touching a ContentResolver.
        LocalReduceMotion provides rememberSystemReduceMotion(),
    ) {
        MaterialTheme(
            colorScheme = when {
                teal && resolvedDark -> DarkTealColorScheme
                teal -> LightTealColorScheme
                resolvedDark -> DarkOrangeColorScheme
                else -> LightOrangeColorScheme
            },
            typography = Typography,
            content = content
        )
    }
}

/** User-turn bubble fill: teal gradient on the default theme, orange logo gradient otherwise. */
@Composable
fun userBubbleBrush(): Brush =
    if (SettingsRepository.settings.themeColor == AppThemeColor.TEAL) TealGradient else BrandGradient
