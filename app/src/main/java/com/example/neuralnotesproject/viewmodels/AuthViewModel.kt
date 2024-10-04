package com.example.neuralnotesproject.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.UserRepository
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    private val _registrationResult = MutableLiveData<Result<Boolean>>()
    val registrationResult: LiveData<Result<Boolean>> get() = _registrationResult

    private val _loginResult = MutableLiveData<Result<Int>>() // Int represents userId
    val loginResult: LiveData<Result<Int>> get() = _loginResult

    init {
        val db = AppDatabase.getDatabase(application)
        repository = UserRepository(db)
    }

    fun register(username: String, password: String, confirmPassword: String) {
        if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _registrationResult.value = Result.failure(Exception("All fields are required."))
            return
        }

        if (password != confirmPassword) {
            _registrationResult.value = Result.failure(Exception("Passwords do not match."))
            return
        }

        if (password.length < 8) {
            _registrationResult.value = Result.failure(Exception("Password must be at least 8 characters long."))
            return
        }

        viewModelScope.launch {
            try {
                // Hash the password
                val passwordHash = hashPassword(password)
                repository.registerUser(username, passwordHash)
                _registrationResult.postValue(Result.success(true))
            } catch (e: Exception) {
                _registrationResult.postValue(Result.failure(Exception("Username already exists.")))
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginResult.value = Result.failure(Exception("Username and password are required."))
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.getUser(username)
                if (user != null && verifyPassword(password, user.passwordHash)) {
                    _loginResult.postValue(Result.success(user.userId))
                } else {
                    _loginResult.postValue(Result.failure(Exception("Invalid credentials.")))
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(Exception("Login failed.")))
            }
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}