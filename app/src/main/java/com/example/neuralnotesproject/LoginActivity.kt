package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.neuralnotesproject.viewmodels.AuthViewModel
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.NotebookRepository
import com.example.neuralnotesproject.repository.NoteRepository
import com.example.neuralnotesproject.repository.UserRepository
import com.example.neuralnotesproject.util.DataMigrationUtil
import kotlinx.coroutines.launch
import com.example.neuralnotesproject.data.AuthResult  // Add this import
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Check if user is already signed in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is already signed in, proceed to MainActivity
            val authResult = AuthResult(
                userId = currentUser.uid,
                email = currentUser.email ?: "",
                username = currentUser.displayName ?: currentUser.email?.substringBefore('@') ?: ""
            )
            navigateToMain(authResult)
            return
        }

        setContentView(R.layout.login_activity)

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvSignup = findViewById(R.id.tv_signup)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            authViewModel.login(username, password)
        }

        tvSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        authViewModel.loginResult.observe(this, Observer { result ->
            result.onSuccess { authResult ->
                navigateToMain(authResult)
            }.onFailure { exception ->
                Toast.makeText(this, "Login failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToMain(authResult: AuthResult) {
        lifecycleScope.launch {
            // Initialize database and repositories
            val database = AppDatabase.getDatabase(applicationContext)
            val userRepository = UserRepository(database.userDao())
            val notebookRepository = NotebookRepository(database.notebookDao())
            val noteRepository = NoteRepository(database.noteDao())

            // Migrate existing data
            DataMigrationUtil.migrateData(
                context = applicationContext,
                userId = authResult.userId,
                userEmail = authResult.email,
                username = authResult.username,
                notebookRepository = notebookRepository,
                noteRepository = noteRepository,
                userRepository = userRepository
            )

            // Start MainActivity
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                putExtra("USER_ID", authResult.userId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
