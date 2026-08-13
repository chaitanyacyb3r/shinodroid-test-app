package com.shinodroid.ssldemo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberMagenta,
    tertiary = CyberGreen,
    background = CyberBlack,
    surface = CyberGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = CyberCyan,
    onSurface = CyberCyan,
    error = CyberRed,
    onError = Color.Black
)

@Composable
fun SSLDemoTheme(
    darkTheme: Boolean = true, // Force dark theme for cyber look
    dynamicColor: Boolean = false, // Disable dynamic colors for consistency
    content: @Composable () -> Unit
) {
    val colorScheme = CyberColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
