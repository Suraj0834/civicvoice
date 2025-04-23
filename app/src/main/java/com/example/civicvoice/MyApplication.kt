package com.example.civicvoice

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        applySavedSettings()
    }

    private fun applySavedSettings() {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Apply language
        val languageCode = prefs.getString("language", "en") ?: "en"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Apply theme
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}