package com.ridvan.target.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.dao.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = (application as TargetApplication).database.userDao()
    private val preferences: AppPreferences = (application as TargetApplication).preferences

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Enter your username and password"
            return
        }
        viewModelScope.launch {
            val user = userDao.getByUsername(trimmedUsername)
            if (user != null && PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                preferences.currentUserId = user.id
                _errorMessage.value = null
                onSuccess()
            } else {
                _errorMessage.value = "Invalid username or password"
            }
        }
    }
}
