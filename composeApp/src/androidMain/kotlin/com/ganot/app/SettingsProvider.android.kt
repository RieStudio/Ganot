package com.ganot.app

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private var _settings: Settings? = null

fun initSettings(context: Context) {
    val sharedPrefs = context.getSharedPreferences("academic_assistant_prefs", Context.MODE_PRIVATE)
    _settings = SharedPreferencesSettings(sharedPrefs)
}

actual fun getSettings(): Settings {
    return _settings ?: throw IllegalStateException("Settings not initialized")
}
