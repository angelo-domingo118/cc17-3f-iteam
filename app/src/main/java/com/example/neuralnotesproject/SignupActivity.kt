package com.example.neuralnotesproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        etUsername = findViewById(R.id.et_signup_username)
        etPassword = findViewById(R.id.et_signup_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSignup = findViewById(R.id.btn_signup)
        tvLogin = findViewById(R.id.tv_login)

        btnSignup.setOnClickListener {
            // For now, just navigate back to LoginActivity
            finish()
        }

        tvLogin.setOnClickListener {
            finish() // This will take us back to LoginActivity
        }
    }
}