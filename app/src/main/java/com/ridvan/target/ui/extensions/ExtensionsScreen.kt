package com.ridvan.target.ui.extensions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ridvan.target.R
import com.ridvan.target.TargetApplication
import com.ridvan.target.data.extensions.LiliesLib
import com.ridvan.target.data.extensions.TargetExtension
import com.ridvan.target.data.modules.AppModule
import com.ridvan.target.ui.common.GroupedCard
import com.ridvan.target.ui.shell.rememberEnabledModules
import kotlinx.coroutines.launch

/** Settings → Extensions: optional companion apps, each switched on per account. For now only Lilies Lib. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as TargetApplication
    val scope = rememberCoroutineScope()
    val enabled by application.extensions.enabled.collectAsState()
    val readingOn = AppModule.READING in rememberEnabledModules()
    // Re-checked on every resume: the user may come back from installing it.
    var installed by remember { mutableStateOf(LiliesLib.isInstalled(context)) }
    LifecycleResumeEffect(Unit) {
        installed = LiliesLib.isInstalled(context)
        onPauseOrDispose { }
    }

    fun setAutoSync(on: Boolean) {
        val uid = application.preferences.currentUserId ?: return
        scope.launch {
            application.extensions.setEnabled(uid, TargetExtension.LILIES_LIB_AUTO_SYNC, on)
            if (on) com.ridvan.target.data.extensions.LiliesSync.sync(application, application.database.bookDao(), uid)
        }
    }
    val turnOnAutoSync = com.ridvan.target.ui.reading.rememberLiliesAccess { setAutoSync(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.extensions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.extensions_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(stringResource(R.string.extensions_lilies_lib), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(if (installed) R.string.extensions_installed else R.string.extensions_not_installed),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.extensions_lilies_lib_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.extensions_use), modifier = Modifier.weight(1f))
                        Switch(
                            checked = TargetExtension.LILIES_LIB in enabled,
                            onCheckedChange = { on ->
                                val uid = application.preferences.currentUserId ?: return@Switch
                                scope.launch { application.extensions.setEnabled(uid, TargetExtension.LILIES_LIB, on) }
                            },
                        )
                    }
                    if (TargetExtension.LILIES_LIB in enabled) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.extensions_auto_sync), modifier = Modifier.weight(1f))
                            Switch(
                                checked = TargetExtension.LILIES_LIB_AUTO_SYNC in enabled,
                                onCheckedChange = { on -> if (on) turnOnAutoSync() else setAutoSync(false) },
                            )
                        }
                        Text(
                            stringResource(R.string.extensions_auto_sync_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!readingOn) {
                        Text(
                            stringResource(R.string.extensions_needs_reading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!installed) {
                        OutlinedButton(
                            onClick = {
                                if (!LiliesLib.openDownload(context)) {
                                    Toast.makeText(context, R.string.extensions_no_browser, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Text(stringResource(R.string.extensions_get_lilies_lib), modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            stringResource(R.string.extensions_get_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
