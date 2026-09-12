package com.ridvan.target.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R

enum class ErrorMessage {
    ENTER_USERNAME_PASSWORD,
    INVALID_USERNAME_PASSWORD,
    FILL_REQUIRED_FIELDS,
    USERNAME_TAKEN,
    FILL_ALL_PASSWORD_FIELDS,
    CURRENT_PASSWORD_INCORRECT,
    NEW_PASSWORDS_DONT_MATCH,
    ENTER_PASSWORD,
    INCORRECT_PASSWORD,
}

@Composable
fun ErrorMessage.text(): String = when (this) {
    ErrorMessage.ENTER_USERNAME_PASSWORD -> stringResource(R.string.error_enter_username_password)
    ErrorMessage.INVALID_USERNAME_PASSWORD -> stringResource(R.string.error_invalid_username_password)
    ErrorMessage.FILL_REQUIRED_FIELDS -> stringResource(R.string.error_fill_required_fields)
    ErrorMessage.USERNAME_TAKEN -> stringResource(R.string.error_username_taken)
    ErrorMessage.FILL_ALL_PASSWORD_FIELDS -> stringResource(R.string.error_fill_all_password_fields)
    ErrorMessage.CURRENT_PASSWORD_INCORRECT -> stringResource(R.string.error_current_password_incorrect)
    ErrorMessage.NEW_PASSWORDS_DONT_MATCH -> stringResource(R.string.error_new_passwords_dont_match)
    ErrorMessage.ENTER_PASSWORD -> stringResource(R.string.error_enter_password)
    ErrorMessage.INCORRECT_PASSWORD -> stringResource(R.string.error_incorrect_password)
}
