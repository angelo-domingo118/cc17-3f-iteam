package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.dao.UserDao
import com.example.neuralnotesproject.data.User

class UserRepository(private val userDao: UserDao) {
    suspend fun insertUser(user: User) = userDao.insertUser(user)
    
    suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
}
