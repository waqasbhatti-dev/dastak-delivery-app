package com.example.ui.theme

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
    primary = DastakPrimaryDark,
    onPrimary = DastakOnPrimaryDark,
    primaryContainer = DastakPrimaryContainerDark,
    secondary = DastakSecondary,
    onSecondary = DastakOnSecondary,
    surface = DastakSurfaceDark,
    onSurface = DastakOnSurfaceDark,
    background = DastakBackgroundDark,
    onBackground = DastakOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = DastakPrimary,
    onPrimary = DastakOnPrimary,
    primaryContainer = DastakPrimaryContainer,
    onPrimaryContainer = DastakOnPrimaryContainer,
    secondary = DastakSecondary,
    onSecondary = DastakOnSecondary,
    secondaryContainer = DastakSecondaryContainer,
    onSecondaryContainer = DastakOnSecondaryContainer,
    tertiary = DastakTertiary,
    onTertiary = DastakOnTertiary,
    tertiaryContainer = DastakTertiaryContainer,
    surface = DastakSurface,
    onSurface = DastakOnSurface,
    surfaceVariant = DastakSurfaceVariant,
    onSurfaceVariant = DastakOnSurfaceVariant,
    background = DastakBackground,
    onBackground = DastakOnBackground,
    outline = DastakOutline,
    outlineVariant = DastakOutlineVariant,
    error = DastakError,
    onError = DastakOnError,
    errorContainer = DastakErrorContainer
)

@Composable
fun DastakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional brand identity colors for Dastak
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
        typography = AppTypography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = DastakTheme(darkTheme, dynamicColor, content)
