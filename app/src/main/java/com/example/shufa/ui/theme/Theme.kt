package com.example.shufa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Brown500,
    onPrimary = PaperWhite,
    primaryContainer = Brown100,
    onPrimaryContainer = Brown900,
    secondary = Gold500,
    onSecondary = InkBlack,
    secondaryContainer = Gold700,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
)

private val DarkColorScheme = darkColorScheme(
    primary = Brown100,
    onPrimary = Brown900,
    primaryContainer = Brown700,
    onPrimaryContainer = Brown100,
    secondary = Gold500,
    onSecondary = InkBlack,
    background = InkBlack,
    onBackground = PaperWhite,
    surface = InkBlack,
    onSurface = PaperWhite,
)

@Composable
fun ShufaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
