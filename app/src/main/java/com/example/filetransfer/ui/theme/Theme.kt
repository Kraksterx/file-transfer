package com.example.filetransfer.ui.theme

import android.app.Activity
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
    primary = md_primary_light,
    onPrimary = md_onPrimary_light,

    secondary = md_secondary_light,

    background = md_background_light,
    onBackground = md_onBackground_light,

    surface = md_surface_light,
    onSurface = md_onSurface_light,

    surfaceVariant = md_surfaceVariant_light,
    outline = md_outline_light
)

private val LightColorScheme = lightColorScheme(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,

    secondary = md_secondary_dark,

    background = md_background_dark,
    onBackground = md_onBackground_dark,

    surface = md_surface_dark,
    onSurface = md_onSurface_dark,

    surfaceVariant = md_surfaceVariant_dark,
    outline = md_outline_dark

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun FileTransferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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