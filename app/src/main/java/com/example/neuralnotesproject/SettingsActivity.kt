package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.neuralnotesproject.firebase.FirebaseAuthManager
import com.example.neuralnotesproject.util.AccountCleanupUtil
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {
    private lateinit var currentEmailTextView: TextView
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        firebaseAuth = FirebaseAuth.getInstance()
        
        // Initialize views
        currentEmailTextView = findViewById(R.id.tv_current_email)
        currentEmailTextView.text = firebaseAuth.currentUser?.email ?: "No email set"

        // Set up click listeners
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        findViewById<TextView>(R.id.btn_change_password).setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<TextView>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        findViewById<TextView>(R.id.btn_delete_account).setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                when {
                    currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                        Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    newPassword.length < 8 -> {
                        Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                // Show loading state
                positiveButton.isEnabled = false
                positiveButton.text = "Changing..."

                lifecycleScope.launch {
                    try {
                        changePassword(currentPassword, newPassword)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        positiveButton.isEnabled = true
                        positiveButton.text = "Change"
                    }
                }
            }
        }

        dialog.show()
    }

    private suspend fun changePassword(currentPassword: String, newPassword: String) {
        try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                try {
                    user.reauthenticate(credential).await()
                } catch (e: Exception) {
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show()
                    return
                }

                // Change password
                user.updatePassword(newPassword).await()
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to change password: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    deleteAccount()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
}
