package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.firebase.FirebaseAuthManager
import com.example.neuralnotesproject.repository.UserRepository
import kotlinx.coroutines.launch
import com.example.neuralnotesproject.util.AccountCleanupUtil
import android.widget.ImageView
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set up back button click listener
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Set up spinner
        val spinner = findViewById<Spinner>(R.id.spinner_ai_model)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ai_model_entries,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter

        // Initialize preferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Set up API key input
        val apiKeyInput = findViewById<TextInputEditText>(R.id.et_api_key)
        apiKeyInput.setText(encryptedPrefs.getString("api_key", ""))
        
        // Set up spinner selection
        val savedModel = encryptedPrefs.getString("selected_ai_model", "gemini_1_0_pro")
        val modelPosition = adapter.getPosition(savedModel)
        if (modelPosition != -1) {
            spinner.setSelection(modelPosition)
        }

        // Set up spinner listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedModel = parent?.getItemAtPosition(position).toString()
                encryptedPrefs.edit().putString("selected_ai_model", selectedModel).apply()
                updateAIModelConfiguration(selectedModel)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up API key save
        apiKeyInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val apiKey = apiKeyInput.text.toString()
                if (isValidApiKey(apiKey)) {
                    encryptedPrefs.edit().putString("api_key", apiKey).apply()
                } else {
                    Toast.makeText(this, R.string.api_key_invalid, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set up logout button
        findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Set up delete account button
        findViewById<MaterialButton>(R.id.btn_delete_account).setOnClickListener {
            showDeleteAccountConfirmationDialog()
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

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
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
        FirebaseAuthManager().signOut()
        navigateToLogin()
    }

    private suspend fun deleteAccount() {
        try {
            val firebaseAuthManager = FirebaseAuthManager()
            val currentUser = firebaseAuthManager.getCurrentUser()
            val userId = currentUser?.uid

            if (userId != null) {
                firebaseAuthManager.deleteAccount()
                    .onSuccess {
                        lifecycleScope.launch {
                            try {
                                AccountCleanupUtil(this@SettingsActivity).cleanupUserData(userId)
                                navigateToLogin()
                                Toast.makeText(this@SettingsActivity, 
                                    "Account deleted successfully", 
                                    Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@SettingsActivity,
                                    "Error cleaning up local data: ${e.message}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .onFailure { exception ->
                        Toast.makeText(this, 
                            "Failed to delete account: ${exception.message}", 
                            Toast.LENGTH_LONG).show()
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, 
                "An error occurred: ${e.message}", 
                Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
