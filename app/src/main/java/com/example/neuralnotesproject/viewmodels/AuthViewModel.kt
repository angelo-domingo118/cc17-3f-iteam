package com.example.neuralnotesproject.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.UserRepository
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.neuralnotesproject.firebase.FirebaseAuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseAuth = FirebaseAuthManager()
    private val _registrationResult = MutableLiveData<Result<Boolean>>()
    val registrationResult: LiveData<Result<Boolean>> get() = _registrationResult
    private val _loginResult = MutableLiveData<Result<String>>() // String represents userId
    val loginResult: LiveData<Result<String>> get() = _loginResult

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _registrationResult.value = Result.failure(Exception("All fields are required."))
            return
        }

        if (password != confirmPassword) {
            _registrationResult.value = Result.failure(Exception("Passwords do not match."))
            return
        }

        viewModelScope.launch {
            try {
                firebaseAuth.signUp(email, password)
                    .onSuccess {
                        _registrationResult.postValue(Result.success(true))
                    }
                    .onFailure {
                        _registrationResult.postValue(Result.failure(it))
                    }
            } catch (e: Exception) {
                _registrationResult.postValue(Result.failure(e))
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = Result.failure(Exception("Email and password are required."))
            return
        }

        viewModelScope.launch {
            try {
                firebaseAuth.signIn(email, password)
                    .onSuccess {
                        _loginResult.postValue(Result.success(it.uid))
                    }
                    .onFailure {
                        _loginResult.postValue(Result.failure(it))
                    }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
            }
        }
    }
}
