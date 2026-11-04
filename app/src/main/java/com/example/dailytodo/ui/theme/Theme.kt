package com.example.dailytodo.ui.theme

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

private val Teal = Color(0xFF0F6E56)
private val TealLight = Color(0xFF5DCAA5)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Color(0xFF534AB7),
    tertiary = Color(0xFF854F0B),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    secondary = Color(0xFFAFA9EC),
    tertiary = Color(0xFFFAC775),
)

@Composable
fun DailyTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Android 12+ 跟随用户壁纸取色，和系统其他应用观感一致
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
