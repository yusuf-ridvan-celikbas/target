package com.ridvan.target.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.local.entity.PreferredNameSource
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.data.resolvePreferredName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserEditViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userDao = targetApplication.database.userDao()
    private val preferences = targetApplication.preferences

    val user: StateFlow<User?> = run {
        val userId = preferences.currentUserId
        if (userId != null) userDao.getById(userId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun save(
        firstName: String,
        middleName: String,
        lastName: String,
        username: String,
        email: String,
        preferredNameSource: PreferredNameSource,
        preferredNameCustomText: String,
        oldPassword: String,
        newPassword: String,
        confirmNewPassword: String,
        onSuccess: () -> Unit,
    ) {
        val trimmedFirst = firstName.trim()
        val trimmedMiddle = middleName.trim()
        val trimmedLast = lastName.trim()
        val trimmedUsername = username.trim()
        if (trimmedFirst.isEmpty() || trimmedLast.isEmpty() || trimmedUsername.isEmpty()) {
            _errorMessage.value = "Fill in all required fields"
            return
        }
        viewModelScope.launch {
            val current = user.value ?: return@launch
            if (trimmedUsername != current.username && userDao.countByUsername(trimmedUsername) > 0) {
                _errorMessage.value = "That username is already taken"
                return@launch
            }

            var newHash = current.passwordHash
            var newSalt = current.passwordSalt
            val wantsPasswordChange = oldPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmNewPassword.isNotEmpty()
            if (wantsPasswordChange) {
                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                    _errorMessage.value = "Fill in all three password fields to change your password"
                    return@launch
                }
                if (!PasswordHasher.verify(oldPassword, current.passwordSalt, current.passwordHash)) {
                    _errorMessage.value = "Current password is incorrect"
                    return@launch
                }
                if (newPassword != confirmNewPassword) {
                    _errorMessage.value = "New passwords don't match"
                    return@launch
                }
                newSalt = PasswordHasher.generateSalt()
                newHash = PasswordHasher.hash(newPassword, newSalt)
            }

            val resolvedPreferredName = resolvePreferredName(
                firstName = trimmedFirst,
                middleName = trimmedMiddle,
                lastName = trimmedLast,
                username = trimmedUsername,
                source = preferredNameSource,
                customText = preferredNameCustomText,
            )

            userDao.update(
                current.copy(
                    firstName = trimmedFirst,
                    middleName = trimmedMiddle.takeIf { it.isNotEmpty() },
                    preferredName = resolvedPreferredName,
                    preferredNameSource = preferredNameSource,
                    lastName = trimmedLast,
                    username = trimmedUsername,
                    email = email.trim().takeIf { it.isNotEmpty() },
                    passwordHash = newHash,
                    passwordSalt = newSalt,
                ),
            )
            _errorMessage.value = null
            onSuccess()
        }
    }
}
