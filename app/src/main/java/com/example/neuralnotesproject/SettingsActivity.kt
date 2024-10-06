package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.preference.ListPreference
import androidx.preference.EditTextPreference
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_preferences_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var encryptedPrefs: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            encryptedPrefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                requireContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val aiModelPreference = findPreference<ListPreference>("ai_model")
            aiModelPreference?.setOnPreferenceChangeListener { _, newValue ->
                val selectedModel = newValue as String
                encryptedPrefs.edit().putString("selected_ai_model", selectedModel).apply()
                updateAIModelConfiguration(selectedModel)
                true
            }

            val apiKeyPreference = findPreference<EditTextPreference>("custom_api_key")
            apiKeyPreference?.setOnPreferenceChangeListener { _, newValue ->
                val apiKey = newValue as String
                if (isValidApiKey(apiKey)) {
                    encryptedPrefs.edit().putString("api_key", apiKey).apply()
                    true
                } else {
                    Toast.makeText(context, R.string.api_key_invalid, Toast.LENGTH_LONG).show()
                    false
                }
            }

            findPreference<Preference>("profile")?.setOnPreferenceClickListener {
                startActivity(Intent(activity, ProfileActivity::class.java))
                true
            }

            findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                true
            }

            findPreference<Preference>("delete_account")?.setOnPreferenceClickListener {
                true
            }
        }

        private fun updateAIModelConfiguration(selectedModel: String) {
            when (selectedModel) {
                "gemini_1_0_pro" -> {}
                "gemini_1_0_pro_vision" -> {}
                "gemini_1_5_flash" -> {}
                "gemini_1_5_pro" -> {}
            }
        }

        private fun isValidApiKey(apiKey: String): Boolean {
            return apiKey.length == 39 && apiKey.startsWith("AIzaSy")
        }
    }
}