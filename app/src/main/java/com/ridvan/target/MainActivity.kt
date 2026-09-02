package com.ridvan.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ridvan.target.ui.examlist.ExamListScreen
import com.ridvan.target.ui.theme.TargetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetTheme {
                ExamListScreen()
            }
        }
    }
}
