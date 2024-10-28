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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.firebase.FirebaseAuthManager
import com.example.neuralnotesproject.repository.UserRepository
import kotlinx.coroutines.launch
import com.example.neuralnotesproject.util.AccountCleanupUtil

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
        private lateinit var firebaseAuthManager: FirebaseAuthManager
        private lateinit var userRepository: UserRepository
        private lateinit var accountCleanupUtil: AccountCleanupUtil

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

            firebaseAuthManager = FirebaseAuthManager()
            val database = AppDatabase.getDatabase(requireContext())
            userRepository = UserRepository(database.userDao())

            accountCleanupUtil = AccountCleanupUtil(requireContext())

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

            setupLogoutPreference()
            setupDeleteAccountPreference()
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

        private fun setupLogoutPreference() {
            findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                showLogoutConfirmationDialog()
                true
            }
        }

        private fun setupDeleteAccountPreference() {
            findPreference<Preference>("delete_account")?.setOnPreferenceClickListener {
                showDeleteAccountConfirmationDialog()
                true
            }
        }

        private fun showLogoutConfirmationDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showDeleteAccountConfirmationDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        deleteAccount()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun logout() {
            firebaseAuthManager.signOut()
            navigateToLogin()
        }

        private suspend fun deleteAccount() {
            try {
                val currentUser = firebaseAuthManager.getCurrentUser()
                val userId = currentUser?.uid

                if (userId != null) {
                    // First delete from Firebase Auth
                    firebaseAuthManager.deleteAccount()
                        .onSuccess {
                            // Then clean up all local data
                            lifecycleScope.launch {
                                try {
                                    accountCleanupUtil.cleanupUserData(userId)
                                    navigateToLogin()
                                    Toast.makeText(requireContext(), 
                                        "Account deleted successfully", 
                                        Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(),
                                        "Error cleaning up local data: ${e.message}",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .onFailure { exception ->
                            Toast.makeText(requireContext(), 
                                "Failed to delete account: ${exception.message}", 
                                Toast.LENGTH_LONG).show()
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), 
                    "An error occurred: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }

        private fun navigateToLogin() {
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
