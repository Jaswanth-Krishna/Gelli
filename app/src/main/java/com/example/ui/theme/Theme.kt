package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val GuardDarkColorScheme = darkColorScheme(
    primary = GuardCyan,
    onPrimary = OnGuardCyan,
    primaryContainer = GuardCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = ShieldEmerald,
    onSecondary = OnShieldEmerald,
    secondaryContainer = ShieldEmeraldContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = CautionAmber,
    onTertiary = ObsidianBackground,
    tertiaryContainer = CautionAmberContainer,
    onTertiaryContainer = TextPrimary,
    error = SosCrimson,
    onError = OnSosCrimson,
    errorContainer = SosCrimsonContainer,
    onErrorContainer = TextPrimary,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle
)

val GuardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlashGetGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GuardDarkColorScheme,
        typography = Typography,
        shapes = GuardShapes,
        content = content
    )
}
