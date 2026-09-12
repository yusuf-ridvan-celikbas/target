package com.ridvan.target.ui.switchaccount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.data.local.dao.UserDao
import com.ridvan.target.data.local.entity.User
import com.ridvan.target.ui.common.ErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SwitchAccountViewModel(application: Application) : AndroidViewModel(application) {
    private val targetApplication = application as TargetApplication
    private val userDao: UserDao = targetApplication.database.userDao()
    private val preferences: AppPreferences = targetApplication.preferences
    private val currentUserId = preferences.currentUserId

    val otherUsers: StateFlow<List<User>> = userDao.getAll()
        .map { users -> users.filter { it.id != currentUserId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage.asStateFlow()

    fun switchTo(user: User, password: String, onSuccess: () -> Unit) {
        if (password.isEmpty()) {
            _errorMessage.value = ErrorMessage.ENTER_PASSWORD
            return
        }
        viewModelScope.launch {
            if (PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                preferences.currentUserId = user.id
                _errorMessage.value = null
                onSuccess()
            } else {
                _errorMessage.value = ErrorMessage.INCORRECT_PASSWORD
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
