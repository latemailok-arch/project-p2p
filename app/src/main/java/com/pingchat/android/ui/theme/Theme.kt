package com.pingchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Standard UI semantics live in Material so stock components and custom PingChat composables
// share one source of truth. LocalPingchatPalette below only supplies app-specific extra colors.
internal val DarkPingchatColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5B21B6),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = DarkPingchatPalette.accentOrange,
    onTertiary = Color.Black,
    background = Color(0xFF120828),
    onBackground = Color(0xFFF3F0FF),
    surface = Color(0xFF1E0A3C),
    onSurface = Color(0xFFF3F0FF),
    surfaceVariant = Color(0xFF2D1B4E),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4A3A5C),
    outlineVariant = Color(0xFF2D1B4E),
    error = Color(0xFFEF4444),
    onError = Color.White
)

internal val LightPingchatColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF5B21B6),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF6D28D9),
    tertiary = LightPingchatPalette.accentOrange,
    onTertiary = Color.White,
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF3F0FF),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE9E7F3),
    outlineVariant = Color(0xFFF3F0FF),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun PingchatTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.System)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkPingchatColorScheme else LightPingchatColorScheme
    val palette = if (shouldUseDark) DarkPingchatPalette else LightPingchatPalette

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    CompositionLocalProvider(LocalPingchatPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Duplicate function removed - only one PingchatTheme function above
