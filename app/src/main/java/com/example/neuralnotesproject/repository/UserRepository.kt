package com.example.neuralnotesproject.repository

import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.data.User

class UserRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()

    suspend fun registerUser(username: String, passwordHash: String): Long {
        val user = User(username = username, passwordHash = passwordHash)
        return userDao.insertUser(user)
    }

    suspend fun getUser(username: String): User? {
        return userDao.getUserByUsername(username)
    }
}