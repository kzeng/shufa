package com.example.shufa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.shufa.navigation.ShufaNavGraph
import com.example.shufa.ui.theme.ShufaTheme
import com.example.shufa.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val darkTheme by themeViewModel.darkTheme.collectAsState()
            ShufaTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                ShufaNavGraph(
                    navController = navController,
                    darkTheme = darkTheme,
                    onToggleTheme = { themeViewModel.toggleDarkTheme() }
                )
            }
        }
    }
}
