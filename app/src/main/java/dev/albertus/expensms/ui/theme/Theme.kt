package dev.albertus.expensms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PastelBlue = Color(0xFF7FDBDA)
val PastelGreen = Color(0xFF98FB98)
val ExpenseRed = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary = PastelBlue,
    secondary = PastelGreen,
    tertiary = ExpenseRed,
    background = Color.White,
    surface = Color(0xFFF0F0F0),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = PastelBlue,
    secondary = PastelGreen,
    tertiary = ExpenseRed,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun ExpenSMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}