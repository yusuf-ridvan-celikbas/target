package com.ridvan.target.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ridvan.target.R

/**
 * The "+ Add" FAB used across every list screen — replaces a literal Text("+") glyph
 * with a real icon and a content description.
 */
@Composable
fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add))
    }
}
