package com.ridvan.target

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridvan.target.data.local.AppPreferences
import com.ridvan.target.ui.navigation.TargetNavHost
import com.ridvan.target.ui.theme.TargetTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = AppPreferences(newBase).appLanguage.value
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale(language.languageTag))
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by (application as TargetApplication).preferences.isDarkMode
                .collectAsStateWithLifecycle()
            TargetTheme(darkTheme = isDarkMode) {
                TargetNavHost()
            }
        }
    }
}
