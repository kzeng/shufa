package com.example.shufa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.shufa.navigation.ShufaNavGraph
import com.example.shufa.ui.theme.ShufaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShufaTheme {
                val navController = rememberNavController()
                ShufaNavGraph(navController = navController)
            }
        }
    }
}
