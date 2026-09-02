package com.ridvan.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridvan.target.ui.navigation.TargetNavHost
import com.ridvan.target.ui.theme.TargetTheme

class MainActivity : ComponentActivity() {
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
