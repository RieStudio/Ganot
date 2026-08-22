package com.ganot.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Deep dark space color palettes with dynamic getters
val DeepBackgroundDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DeepSurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val DeepCardDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primaryContainer

@Composable
@ReadOnlyComposable
fun isDarkThemeActive(): Boolean = MaterialTheme.colorScheme.background == Color(0xFF0D0D0D)

// Adaptive content colors
val AdaptWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color.White else Color(0xFF111827) // Premium Night Blue

val AdaptLightGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color.LightGray else Color(0xFF4B5563) // Cool Gray

val AdaptGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color.Gray else Color(0xFF6B7280) // Gray 500

val AdaptDividerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)

val NeonPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFFA855F7) else Color(0xFF2563EB) // Safir mavi

val ElectricBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFF3B82F6) else Color(0xFF059669) // Derin nane yeşili

val NeonBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFF2563EB) else Color(0xFF2563EB)

val MintGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFF10B981) else Color(0xFF10B981) // Taze yeşil

val AlertOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFFF97316) else Color(0xFFF59E0B) // Sıcak gün batımı

val WarningRed: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkThemeActive()) Color(0xFFEF4444) else Color(0xFFEF4444)

val BackgoundGradient: Brush
    @Composable
    get() = if (isDarkThemeActive()) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D0D0D),
                Color(0xFF161616)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF4F6F8),
                Color(0xFFEEF2F6)
            )
        )
    }

val GlowPurpleGradient: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            NeonPurple,
            ElectricBlue
        )
    )

val GlowBlueCyanGradient: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            ElectricBlue,
            Color(0xFF06B6D4)
        )
    )

val WarningRedGradient: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            WarningRed,
            AlertOrange
        )
    )

val MintGreenGradient: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = listOf(
            MintGreen,
            ElectricBlue
        )
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isDarkThemeActive()
    
    // Apple-style Frosted Glass Colors (Higher translucency)
    val bgColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.72f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.70f)
    }
    
    // 1px bright/translucent border for the "glass edge" look
    val glassBorderColor = if (isDark) {
        if (borderColor == Color.Unspecified) Color.White.copy(alpha = 0.18f) else borderColor
    } else {
        if (borderColor == Color.Unspecified) Color.White.copy(alpha = 0.50f) else borderColor
    }
    
    // Gradient border: bright on top/left, subtle on bottom/right
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glassBorderColor,
            glassBorderColor.copy(alpha = 0.05f)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset.Infinite
    )

    // Premium Shadow for depth
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.08f)
    val shadowElevation = 16.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(bgColor)
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = shape
            )
    ) {
        content()
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonPurple,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isDarkThemeActive()
    val bgColor = if (isDark) DeepCardDark else Color(0xFFFFFFFF)
    
    val finalBorderColor = if (isDark) {
        glowColor.copy(alpha = 0.12f) // keep it extremely eye-friendly in dark mode
    } else {
        Color(0xFFE5E7EB) // soft borders in light mode
    }
    
    val borderColors = if (isDark) {
        listOf(
            finalBorderColor,
            Color.Transparent,
            finalBorderColor.copy(alpha = 0.02f)
        )
    } else {
        listOf(
            finalBorderColor,
            Color(0xFFEEF2F6)
        )
    }

    val finalModifier = if (isDark) {
        modifier
    } else {
        modifier.shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.01f),
            spotColor = Color.Black.copy(alpha = 0.03f)
        )
    }

    Box(
        modifier = finalModifier
            .clip(shape)
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = borderColors),
                shape = shape
            )
    ) {
        content()
    }
}
