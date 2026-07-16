package com.vsdigitallabs.pocketpaise.stdbt.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = StudentSecondaryDark,
    onSecondary = StudentOnSecondaryDark,
    secondaryContainer = StudentSecondaryContainerDark,
    onSecondaryContainer = StudentOnSecondaryContainerDark,
    tertiary = MarigoldTertiaryDark,
    onTertiary = MarigoldOnTertiaryDark,
    tertiaryContainer = MarigoldTertiaryContainerDark,
    onTertiaryContainer = MarigoldOnTertiaryContainerDark,
    background = FinanceBackgroundDark,
    onBackground = FinanceOnBackgroundDark,
    surface = FinanceSurfaceDark,
    onSurface = FinanceOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = StudentSecondary,
    onSecondary = StudentOnSecondary,
    secondaryContainer = StudentSecondaryContainer,
    onSecondaryContainer = StudentOnSecondaryContainer,
    tertiary = MarigoldTertiary,
    onTertiary = MarigoldOnTertiary,
    tertiaryContainer = MarigoldTertiaryContainer,
    onTertiaryContainer = MarigoldOnTertiaryContainer,
    background = FinanceBackground,
    onBackground = FinanceOnBackground,
    surface = FinanceSurface,
    onSurface = FinanceOnSurface
)

@Composable
fun PocketPaiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set false to prioritize our custom Emerald student branding!
    content: @Composable () -> Unit
) {
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
        typography = Typography,
        content = content
    )
}
