package com.pingchat.android.ui.discord

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ColorOptionsStore {
    private const val PREFS = "pingchat_color_prefs"
    private const val KEY_ACCENT = "accent_color"

    private val _accent = MutableStateFlow(AccentColor.Blurple)
    val accent: StateFlow<AccentColor> = _accent

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_ACCENT, AccentColor.Blurple.name) ?: AccentColor.Blurple.name
        _accent.value = try { AccentColor.valueOf(name) } catch (_: Exception) { AccentColor.Blurple }
    }

    fun setAccent(context: Context, accent: AccentColor) {
        _accent.value = accent
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACCENT, accent.name).apply()
    }
}
