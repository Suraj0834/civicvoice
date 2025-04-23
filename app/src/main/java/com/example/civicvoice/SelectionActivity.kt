package com.example.civicvoice

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.civicvoice.databinding.ActivitySelectionBinding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class SelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectionBinding
    private var settingsDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonListeners()
        setupSettingsButton()
    }

    private fun setupButtonListeners() {
        binding.registerButton.setOnClickListener {
            animateClick(it)
            openActivity(RegisterTypeActivity::class.java)
        }
        binding.signInButton.setOnClickListener {
            animateClick(it)
            openActivity(LoginActivity::class.java)
        }
    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            animateClick(it)
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        settingsDialog?.dismiss()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val themeSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.themeSwitch)
        val languageSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.languageSpinner)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)

        // Setup theme switch
        val isDarkMode = getSharedPreferences("AppSettings", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode

        // Setup language spinner
        val languages = listOf(
            "English" to "en",
            "Hindi" to "hi",
            "Spanish" to "es"
        )
        val languageNames = languages.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageNames)
        languageSpinner.setAdapter(adapter)

        // Set current language
        val currentLanguage = getSharedPreferences("AppSettings", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        val selectedLanguageIndex = languages.indexOfFirst { it.second == currentLanguage }
        if (selectedLanguageIndex >= 0) {
            languageSpinner.setText(languageNames[selectedLanguageIndex], false)
        }

        languageSpinner.setOnClickListener { languageSpinner.showDropDown() }
        languageSpinner.setOnItemClickListener { _, _, position, _ ->
            // Store selected position (not strictly necessary since we save on button click)
        }

        settingsDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            settingsDialog?.dismiss()
        }

        btnSave.setOnClickListener {
            val selectedLanguageName = languageSpinner.text.toString()
            val selectedLanguageCode = languages.find { it.first == selectedLanguageName }?.second ?: "en"
            val isDarkModeEnabled = themeSwitch.isChecked

            // Save preferences
            val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
            prefs.edit().apply {
                putString("language", selectedLanguageCode)
                putBoolean("dark_mode", isDarkModeEnabled)
                apply()
            }

            // Apply language
            val locale = Locale(selectedLanguageCode)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Apply theme
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            Log.d("SelectionActivity", "Saved language: $selectedLanguageCode, dark mode: $isDarkModeEnabled")

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            settingsDialog?.dismiss()
            recreate()
        }

        settingsDialog?.setOnDismissListener {
            settingsDialog = null
        }

        settingsDialog?.show()
    }

    private fun animateClick(view: android.view.View) {
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f).apply { duration = 150 }.start()
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f).apply { duration = 150 }.start()
    }

    private fun openActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}