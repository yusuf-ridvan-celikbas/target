package com.ridvan.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ridvan.target.ui.navigation.TargetNavHost
import com.ridvan.target.ui.theme.TargetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetTheme {
                TargetNavHost()
            }
        }
    }
}
