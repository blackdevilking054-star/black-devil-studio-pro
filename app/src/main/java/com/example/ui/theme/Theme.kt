package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val XonixDarkColorScheme = darkColorScheme(
    primary = CrimsonNeon,
    onPrimary = Color.White,
    primaryContainer = CrimsonNeonContainer,
    onPrimaryContainer = CrimsonNeonBright,
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = NeonVioletContainer,
    onSecondaryContainer = NeonViolet,
    tertiary = EmeraldSafe,
    onTertiary = OledBlack,
    tertiaryContainer = EmeraldSafeContainer,
    onTertiaryContainer = EmeraldSafe,
    error = SignalRed,
    onError = Color.White,
    errorContainer = SignalRedContainer,
    onErrorContainer = SignalRed,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = DeepObsidian,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = DeepObsidian,
    surfaceContainer = StudioSurface,
    surfaceContainerHigh = StudioSurfaceElevated,
    surfaceContainerHighest = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle
)

val XonixShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun XonixStudioProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = XonixDarkColorScheme,
        typography = XonixTypography,
        shapes = XonixShapes,
        content = content
    )
}
