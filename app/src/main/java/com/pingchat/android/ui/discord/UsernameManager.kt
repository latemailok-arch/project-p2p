package com.pingchat.android.ui.discord

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UsernameManager {
    private const val PREFS = "pingchat_user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_COLOR = "user_color"

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _username.value = prefs.getString(KEY_USERNAME, null)
    }

    fun getUsername(context: Context): String? {
        if (_username.value == null) {
            _username.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)
        }
        return _username.value
    }

    fun setUsername(context: Context, name: String) {
        val clean = name.trim().take(20).replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
        if (clean.length < 2) return
        _username.value = clean
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_USERNAME, clean).apply()
    }

    fun isUsernameSet(context: Context): Boolean = getUsername(context) != null
}
