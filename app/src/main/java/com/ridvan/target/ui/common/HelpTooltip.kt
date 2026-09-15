package com.ridvan.target.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A small (?) icon that pops a short explanation on tap — for genuinely
 * non-obvious behavior, not decoration. Tap-to-show (not the default
 * long-press/hover trigger) via a manual tooltipState.show() call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTooltip(bodyRes: Int, contentDescriptionRes: Int, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { RichTooltip { Text(stringResource(bodyRes)) } },
        state = tooltipState,
        modifier = modifier,
    ) {
        IconButton(onClick = { scope.launch { tooltipState.show() } }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(contentDescriptionRes),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
