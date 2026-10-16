package com.tigermatrix.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC96A00),
    onPrimary = Color.White,
    secondary = Color(0xFF5D4037),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA040),
    onPrimary = Color(0xFF2B1700),
    secondary = Color(0xFFD7B8A8),
    background = Color(0xFF121212),
    surface = Color(0xFF1A1A1A)
)

/** themeMode: 0 = حسب النظام، 1 = فاتح، 2 = داكن */
@Composable
fun TigerMatrixTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
