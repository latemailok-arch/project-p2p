package com.pingchat.android.ui.discord

import androidx.compose.ui.graphics.Color

// Discord faithful palette + PingChat extensions
object DiscordPalette {
    val DarkBackground = Color(0xFF313338) // Discord main chat
    val DarkSidebar = Color(0xFF2B2D31) // channel list
    val DarkRail = Color(0xFF232428) // server rail
    val DarkInput = Color(0xFF383A40)
    val DarkCard = Color(0xFF2B2D31)
    val Blurple = Color(0xFF5865F2)
    val BlurpleHover = Color(0xFF4752C4)
    val Green = Color(0xFF23A559)
    val Yellow = Color(0xFFFEE75C)
    val Red = Color(0xFFF23F43)
    val Grey100 = Color(0xFFF2F3F5)
    val Grey300 = Color(0xFFB5BAC1)
    val Grey400 = Color(0xFF949BA4)
}

enum class AccentColor(val displayName: String, val color: Color) {
    Blurple("Blurple", DiscordPalette.Blurple),
    Green("Green", DiscordPalette.Green),
    Fuchsia("Fuchsia", Color(0xFFEB459E)),
    Yellow("Yellow", Color(0xFFFEE75C)),
    Orange("Orange", Color(0xFFF97316)),
    Teal("Teal", Color(0xFF14B8A6)),
    Purple("Ping Purple", Color(0xFF7C3AED))
}
