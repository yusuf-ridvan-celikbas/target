package com.ridvan.target.ui.auth

import com.ridvan.target.data.PasswordHasher
import com.ridvan.target.data.RecoveryCode
import com.ridvan.target.data.local.dao.UserDao

object RecoveryCodes {
    /** Generates and stores a new code (replacing any old one); returns it formatted for display. */
    suspend fun issue(userDao: UserDao, userId: Long): String {
        val code = RecoveryCode.generate()
        val salt = PasswordHasher.generateSalt()
        userDao.setRecovery(userId, RecoveryCode.hash(code, salt), salt)
        return RecoveryCode.format(code)
    }
}
