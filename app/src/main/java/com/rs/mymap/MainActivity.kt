package com.rs.mymap

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rs.mymap.data.ThemePreferences
import com.rs.mymap.ui.MapScreen
import com.rs.mymap.ui.theme.MyMapTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Theme from SharedPreferences
        val themePreferences = ThemePreferences(this)
        val mode = if (themePreferences.isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        enableEdgeToEdge()
        setContent {
            MyMapTheme(darkTheme = themePreferences.isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
