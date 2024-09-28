package dev.albertus.expensms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PastelBlue = Color(0xFF7FDBDA)
val PastelGreen = Color(0xFF98FB98)
val ExpenseRed = Color(0xFFFF6B6B)

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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}