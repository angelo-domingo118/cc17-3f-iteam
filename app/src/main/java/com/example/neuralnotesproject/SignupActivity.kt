package com.example.neuralnotesproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.neuralnotesproject.viewmodels.AuthViewModel

class SignupActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        etUsername = findViewById(R.id.et_signup_username)
        etPassword = findViewById(R.id.et_signup_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSignup = findViewById(R.id.btn_signup)
        tvLogin = findViewById(R.id.tv_login)

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            authViewModel.register(username, password, confirmPassword)
        }

        tvLogin.setOnClickListener {
            finish() // Navigate back to LoginActivity
        }

        authViewModel.registrationResult.observe(this, Observer { result ->
            result.onSuccess {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                finish() // Navigate back to LoginActivity
            }
            result.onFailure { exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}