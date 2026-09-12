package com.salarytracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.salarytracker.app.ui.theme.DeepTeal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalaryTrackerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun SalaryTrackerTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = DeepTeal
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
