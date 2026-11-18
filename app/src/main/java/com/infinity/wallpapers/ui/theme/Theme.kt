package com.infinity.wallpapers.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2350), onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF2DE2C0), onSecondary = Color(0xFF00382E),
    background = Color(0xFF0A0A0F), onBackground = Color(0xFFEDEDF5),
    surface = Color(0xFF14141B), onSurface = Color(0xFFEDEDF5),
    surfaceVariant = Color(0xFF1E1E28), onSurfaceVariant = Color(0xFFB4B4C4),
    outline = Color(0xFF3A3A4A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B45F0), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF), onPrimaryContainer = Color(0xFF1B0F6B),
    secondary = Color(0xFF008F78), onSecondary = Color.White,
    background = Color(0xFFF7F7FB), onBackground = Color(0xFF14141B),
    surface = Color.White, onSurface = Color(0xFF14141B),
    surfaceVariant = Color(0xFFE9E9F2), onSurfaceVariant = Color(0xFF55556A),
    outline = Color(0xFFC4C4D4),
)

@Composable
fun InfinityTheme(dark: Boolean, content: @Composable () -> Unit) {
    val base = Typography()
    val typography = base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
