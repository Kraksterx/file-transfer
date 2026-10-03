package com.example.filetransfer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = md_primary_light,
    onPrimary = md_onPrimary_light,
    secondary = md_secondary_light,
    onSecondary = md_onSecondary_light,
    background = md_background_light,
    onBackground = md_onBackground_light,
    surface = md_surface_light,
    onSurface = md_onSurface_light,
    surfaceVariant = md_surfaceVariant_light,
    outline = md_outline_light
)

private val DarkColorScheme = darkColorScheme(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,
    secondary = md_secondary_dark,
    onSecondary = md_onSecondary_dark,
    background = md_background_dark,
    onBackground = md_onBackground_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark,
    surfaceVariant = md_surfaceVariant_dark,
    outline = md_outline_dark
)

@Composable
fun FileTransferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val semanticColors = if (darkTheme) {
        SemanticColors(
            success = md_success_dark,
            error = md_error_dark,
            warning = md_warning_dark
        )
    } else {
        SemanticColors(
            success = md_success_light,
            error = md_error_light,
            warning = md_warning_light
        )
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
