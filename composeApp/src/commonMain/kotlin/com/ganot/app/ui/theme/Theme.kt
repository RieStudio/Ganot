package com.ganot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7), // NeonPurple
    secondary = Color(0xFF3B82F6), // ElectricBlue
    tertiary = Color(0xFF10B981), // MintGreen
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF161616),
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),     // Canlı / oturaklı safir mavi
    secondary = Color(0xFF059669),   // Derin nane yeşili
    tertiary = Color(0xFF10B981),    // Soft taze yeşil (güvenli)
    background = Color(0xFFF4F6F8),  // Sistem zemini - soft krem/gri
    surface = Color(0xFFFFFFFF),     // Saf beyaz kartlar/kutular
    onBackground = Color(0xFF111827), // Derin antrasit
    onSurface = Color(0xFF111827),
    primaryContainer = Color(0xFFFFFFFF), // Saf beyaz
    onPrimaryContainer = Color(0xFF111827),
    surfaceVariant = Color(0xFFE5E7EB),   // İnce sınır rengi (#E5E7EB)
    onSurfaceVariant = Color(0xFF4B5563)  // İkincil metinler (#4B5563)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
