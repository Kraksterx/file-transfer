package com.example.filetransfer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val md_primary_light = Color(0xFF1A1A1A)
val md_onPrimary_light = Color(0xFFFFFFFF)
val md_secondary_light = Color(0xFF474747)
val md_onSecondary_light = Color(0xFFFFFFFF)
val md_background_light = Color(0xFFFCFCFC)
val md_onBackground_light = Color(0xFF1A1A1A)
val md_surface_light = Color(0xFFFFFFFF)
val md_onSurface_light = Color(0xFF1A1A1A)
val md_surfaceVariant_light = Color(0xFFE0E0E0)
val md_outline_light = Color(0xFF8F8F8F)

val md_primary_dark = Color(0xFFC4C4C4)
val md_onPrimary_dark = Color(0xFF1A1A1A)
val md_secondary_dark = Color(0xFFA9A9A9)
val md_onSecondary_dark = Color(0xFF1A1A1A)
val md_background_dark = Color(0xFF121212)
val md_onBackground_dark = Color(0xFFE0E0E0)
val md_surface_dark = Color(0xFF1E1E1E)
val md_onSurface_dark = Color(0xFFE0E0E0)
val md_surfaceVariant_dark = Color(0xFF2C2C2C)
val md_outline_dark = Color(0xFF8F8F8F)

val md_success_light = Color(0xFF2E7D32)
val md_error_light = Color(0xFFB3261E)
val md_warning_light = Color(0xFF8F5000)

val md_success_dark = Color(0xFF81C995)
val md_error_dark = Color(0xFFF2B8B5)
val md_warning_dark = Color(0xFFFFB868)

@Immutable
data class SemanticColors(
    val success: Color,
    val error: Color,
    val warning: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        success = md_success_light,
        error = md_error_light,
        warning = md_warning_light
    )
}
