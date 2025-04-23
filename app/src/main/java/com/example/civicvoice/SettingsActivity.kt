package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.civicvoice.databinding.ActivitySettingsBinding
import com.example.civicvoice.utils.UserSession
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSettings()
        setupClickListeners()
    }

    private fun setupSettings() {
        // Theme switch
        val isDarkMode = getSharedPreferences("AppSettings", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        binding.themeSwitch.isChecked = isDarkMode
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("AppSettings", MODE_PRIVATE).edit()
                .putBoolean("dark_mode", isChecked)
                .apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Language spinner
        val languages = listOf(
            "English" to "en",
            "Hindi" to "hi",
            "Spanish" to "es"
        )
        val languageNames = languages.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageNames)
        binding.languageSpinner.setAdapter(adapter)

        // Set current language
        val currentLanguage = getSharedPreferences("AppSettings", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        val selectedLanguageIndex = languages.indexOfFirst { it.second == currentLanguage }
        if (selectedLanguageIndex >= 0) {
            binding.languageSpinner.setText(languageNames[selectedLanguageIndex], false)
        }

        binding.languageSpinner.setOnItemClickListener { _, _, position, _ ->
            val selectedLanguageCode = languages[position].second
            getSharedPreferences("AppSettings", MODE_PRIVATE).edit()
                .putString("language", selectedLanguageCode)
                .apply()

            // Apply language
            val locale = Locale(selectedLanguageCode)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Recreate activity to apply changes
            recreate()
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            // Preferences are saved automatically via listeners
            finish()
        }

        binding.logoutButton.setOnClickListener {
            UserSession.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}