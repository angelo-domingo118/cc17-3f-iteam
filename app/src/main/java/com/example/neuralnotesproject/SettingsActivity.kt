package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
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
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

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
            showDeleteAccountDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()

        // Remove any default background from the dialog window
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Find views
        val currentPassword = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val newPassword = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val confirmPassword = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnChange = dialogView.findViewById<MaterialButton>(R.id.btn_change)

        // Set click listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnChange.setOnClickListener {
            val currentPw = currentPassword.text.toString()
            val newPw = newPassword.text.toString()
            val confirmPw = confirmPassword.text.toString()
            
            if (validatePasswordInputs(currentPw, newPw, confirmPw)) {
                lifecycleScope.launch {
                    handlePasswordChange(currentPw, newPw, dialog)
                }
            }
        }

        dialog.show()
    }

    private suspend fun handlePasswordChange(currentPassword: String, newPassword: String, dialog: AlertDialog) {
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
                dialog.dismiss()
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

    private fun showDeleteAccountDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null)
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()

        // Remove any default background from the dialog window
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Find views
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

        // Set click listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val password = passwordInput.text.toString()
            if (password.isNotEmpty()) {
                lifecycleScope.launch {
                    handleAccountDeletion(password, dialog)
                }
            } else {
                passwordInput.error = "Password is required"
            }
        }

        dialog.show()
    }

    private suspend fun handleAccountDeletion(password: String, dialog: AlertDialog) {
        try {
            val user = firebaseAuth.currentUser
            if (user != null && user.email != null) {
                // Re-authenticate and delete account
                val firebaseAuthManager = FirebaseAuthManager()
                firebaseAuthManager.reauthenticateAndDelete(user.email!!, password)
                    .onSuccess {
                        // Clean up local data
                        val userId = user.uid
                        AccountCleanupUtil(this@SettingsActivity).cleanupUserData(userId)
                        
                        // Navigate to login screen
                        navigateToLogin()
                        
                        Toast.makeText(
                            this@SettingsActivity,
                            "Account deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .onFailure { exception ->
                        Toast.makeText(
                            this@SettingsActivity,
                            "Failed to delete account: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@SettingsActivity,
                "Authentication failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validatePasswordInputs(currentPw: String, newPw: String, confirmPw: String): Boolean {
        when {
            currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty() -> {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return false
            }
            newPw != confirmPw -> {
                Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show()
                return false
            }
            newPw.length < 8 -> {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }
}
