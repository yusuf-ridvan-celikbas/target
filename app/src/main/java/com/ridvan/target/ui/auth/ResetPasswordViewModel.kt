package com.ridvan.target.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.RecoveryCode
import com.ridvan.target.ui.common.ErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = (application as TargetApplication).database.userDao()

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage.asStateFlow()

    /** The replacement code, set once after a successful reset. */
    private val _newCode = MutableStateFlow<String?>(null)
    val newCode: StateFlow<String?> = _newCode.asStateFlow()

    fun reset(username: String, code: String, newPassword: String, confirm: String) {
        if (username.isBlank() || code.isBlank() || newPassword.isEmpty() || confirm.isEmpty()) {
            _errorMessage.value = ErrorMessage.FILL_REQUIRED_FIELDS
            return
        }
        if (newPassword != confirm) {
            _errorMessage.value = ErrorMessage.NEW_PASSWORDS_DONT_MATCH
            return
        }
        viewModelScope.launch {
            val user = userDao.getByUsername(username.trim())
            if (user == null) {
                _errorMessage.value = ErrorMessage.UNKNOWN_USER
                return@launch
            }
            if (user.recoveryHash == null || user.recoverySalt == null) {
                _errorMessage.value = ErrorMessage.NO_RECOVERY_CODE
                return@launch
            }
            if (!RecoveryCode.verify(code, user.recoverySalt, user.recoveryHash)) {
                _errorMessage.value = ErrorMessage.WRONG_RECOVERY_CODE
                return@launch
            }
            val salt = PasswordHasher.generateSalt()
            userDao.setPassword(user.id, PasswordHasher.hash(newPassword, salt), salt)
            _newCode.value = RecoveryCodes.issue(userDao, user.id)
            _errorMessage.value = null
        }
    }
}
