package com.henryliu.cbtreframe.shared

import com.russhwolf.settings.Settings

class SettingsManager(private val settings: Settings) {
    fun saveApiKey(key: String) {
        settings.putString("api_key", key)
    }

    fun getApiKey(): String? {
        val key = settings.getString("api_key", "")
        return if (key.isNotEmpty()) key else null
    }
}
