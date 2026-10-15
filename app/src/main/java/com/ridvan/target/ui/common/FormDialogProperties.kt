package com.ridvan.target.ui.common

import androidx.compose.ui.window.DialogProperties

/** For dialogs holding user input: Back still cancels, but an outside tap won't discard the form. */
val FormDialogProperties = DialogProperties(dismissOnClickOutside = false)
