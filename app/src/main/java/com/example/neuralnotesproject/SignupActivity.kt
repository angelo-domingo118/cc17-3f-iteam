package com.example.neuralnotesproject

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.neuralnotesproject.viewmodels.AuthViewModel
import com.google.android.material.textfield.TextInputEditText

class SignupActivity : AppCompatActivity() {
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        etEmail = findViewById(R.id.et_signup_email)
        etPassword = findViewById(R.id.et_signup_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSignup = findViewById(R.id.btn_signup)
        tvLogin = findViewById(R.id.tv_login)

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            authViewModel.register(email, password, confirmPassword)
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