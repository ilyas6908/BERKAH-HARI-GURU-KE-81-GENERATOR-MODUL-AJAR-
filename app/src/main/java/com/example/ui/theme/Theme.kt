package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkEmeraldOnContainer,
    secondary = DarkSaffronSecondary,
    onSecondary = DarkSaffronOnSecondary,
    secondaryContainer = DarkSaffronContainer,
    onSecondaryContainer = DarkSaffronOnContainer,
    tertiary = DarkIndigoTertiary,
    onTertiary = DarkIndigoOnTertiary,
    tertiaryContainer = DarkIndigoContainer,
    onTertiaryContainer = DarkIndigoOnContainer,
    background = DarkBackground,
    onBackground = DarkInkOnSurface,
    surface = DarkSurface,
    onSurface = DarkInkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = SaffronSecondary,
    onSecondary = SaffronOnSecondary,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = SaffronOnContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoOnContainer,
    background = CreamBackground,
    onBackground = InkOnSurface,
    surface = CreamSurface,
    onSurface = InkOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = SubtleOutline
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AjarMerdekaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
