package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference<Preference>("profile")?.setOnPreferenceClickListener {
                startActivity(Intent(activity, ProfileActivity::class.java))
                true
            }

            findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                // Implement logout logic here
                // For example:
                // (activity as? MainActivity)?.logout()
                true
            }

            findPreference<Preference>("delete_account")?.setOnPreferenceClickListener {
                // Implement delete account logic here
                true
            }
        }
    }
}