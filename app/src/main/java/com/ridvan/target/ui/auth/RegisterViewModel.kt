package com.ridvan.target.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.dao.UserDao
import com.ridvan.target.data.local.entity.PreferredNameSource
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.data.resolvePreferredName
import com.ridvan.target.ui.common.ErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = (application as TargetApplication).database.userDao()
    private val preferences: AppPreferences = (application as TargetApplication).preferences

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage.asStateFlow()

    fun register(
        firstName: String,
        middleName: String,
        lastName: String,
        username: String,
        email: String,
        preferredNameSource: PreferredNameSource,
        preferredNameCustomText: String,
        password: String,
        onSuccess: () -> Unit,
    ) {
        val trimmedFirst = firstName.trim()
        val trimmedMiddle = middleName.trim()
        val trimmedLast = lastName.trim()
        val trimmedUsername = username.trim()
        if (trimmedFirst.isEmpty() || trimmedLast.isEmpty() || trimmedUsername.isEmpty() || password.isEmpty()) {
            _errorMessage.value = ErrorMessage.FILL_REQUIRED_FIELDS
            return
        }
        viewModelScope.launch {
            if (userDao.countByUsername(trimmedUsername) > 0) {
                _errorMessage.value = ErrorMessage.USERNAME_TAKEN
                return@launch
            }
            val salt = PasswordHasher.generateSalt()
            val resolvedPreferredName = resolvePreferredName(
                firstName = trimmedFirst,
                middleName = trimmedMiddle,
                lastName = trimmedLast,
                username = trimmedUsername,
                source = preferredNameSource,
                customText = preferredNameCustomText,
            )
            val user = User(
                firstName = trimmedFirst,
                middleName = trimmedMiddle.takeIf { it.isNotEmpty() },
                preferredName = resolvedPreferredName,
                preferredNameSource = preferredNameSource,
                lastName = trimmedLast,
                username = trimmedUsername,
                email = email.trim().takeIf { it.isNotEmpty() },
                passwordHash = PasswordHasher.hash(password, salt),
                passwordSalt = salt,
            )
            val id = userDao.insert(user)
            preferences.currentUserId = id
            _errorMessage.value = null
            onSuccess()
        }
    }
}
