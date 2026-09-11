package com.pingchat.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * PingChat-specific color tokens that do not have a faithful Material 3 semantic role.
 *
 * Standard backgrounds, surfaces, text, outlines, primary/secondary accents, and errors belong
 * to [androidx.compose.material3.MaterialTheme.colorScheme]. Keeping only the extra app semantics
 * here lets Material components inherit correct defaults without losing PingChat's identity.
 */
@Immutable
data class PingchatPalette(
    // MARK: - Form controls
    /**
     * Resting border for text inputs. Deliberately a neutral grey rather than the purple-tinted
     * Material outline: the composer is the one surface the user stares at while typing.
     */
    val inputOutline: Color,
    /** Border for a focused text input. Purple accent. */
    val inputOutlineFocused: Color,
    /**
     * Fill for text inputs. Light purple tint for visual cohesion.
     */
    val inputSurface: Color,
    /** Fill for a focused text input. Slightly lighter. */
    val inputSurfaceFocused: Color,
    /** Resting disc behind the composer's action glyphs. Light purple. */
    val inputButton: Color,

    // MARK: - Extra semantics
    /** Timestamps, placeholders, section labels, disabled states. */
    val textTertiary: Color,
    /** Self, mentions targeting you, unread DMs. */
    val accentOrange: Color,
    /** Nostr reachability - now using primary purple theme. */
    val accentPurple: Color,

    // MARK: - Deterministic peer colors
    /**
     * Saturation/value applied after deriving a peer's stable hue. Swap this when adding a
     * new theme — see [PeerColorStyle] for contrast guidelines.
     */
    val peerColors: PeerColorStyle,
)

val DarkPingchatPalette = PingchatPalette(
    inputOutline = Color(0xFF4A3A5C),
    inputOutlineFocused = Color(0xFF8B5CF6),
    inputSurface = Color(0xFF1E0A3C),
    inputSurfaceFocused = Color(0xFF2D1B4E),
    inputButton = Color(0xFF2D1B4E),
    textTertiary = Color(0xFF9CA3AF),
    accentOrange = Color(0xFFFF9F0A),
    accentPurple = Color(0xFFA78BFA),
    peerColors = PeerColorStyle.Dark,
)

val LightPingchatPalette = PingchatPalette(
    inputOutline = Color(0xFFD1D5DB),
    inputOutlineFocused = Color(0xFF7C3AED),
    inputSurface = Color(0xFFF3F0FF),
    inputSurfaceFocused = Color(0xFFEDE9FE),
    inputButton = Color(0xFFEDE9FE),
    textTertiary = Color(0xFF6B7280),
    accentOrange = Color(0xFFFF9500),
    accentPurple = Color(0xFF7C3AED),
    peerColors = PeerColorStyle.Light,
)

val LocalPingchatPalette = staticCompositionLocalOf { DarkPingchatPalette }

/**
 * Motion tokens. The redesign leans on short, snappy transitions: long durations read as
 * sluggish on a chat surface where the user is scanning quickly.
 */
object PingchatMotion {
    /** Icon tints, text colors, small fills. */
    const val QUICK_MS = 120

    /** Tab indicators, pill growth, chip reveals. */
    const val STANDARD_MS = 180

    /** Sheet-level fades and scroll-driven top bars. */
    const val EMPHASIZED_MS = 240
}
