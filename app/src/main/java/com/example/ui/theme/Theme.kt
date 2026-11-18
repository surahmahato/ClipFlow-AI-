package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ClipFlowLightColorScheme = lightColorScheme(
    primary = PrimaryGoldButton,
    onPrimary = InkCharcoal,
    primaryContainer = WarmAccentYellow,
    onPrimaryContainer = InkCharcoal,
    secondary = WarmChipSurface,
    onSecondary = InkCharcoal,
    secondaryContainer = SoftCreamSurface,
    onSecondaryContainer = InkCharcoal,
    tertiary = ViralMintBg,
    onTertiary = ViralMintText,
    background = CreamBackground,
    onBackground = InkCharcoal,
    surface = PureWhite,
    onSurface = InkCharcoal,
    surfaceVariant = SoftCreamSurface,
    onSurfaceVariant = SecondarySlate,
    outline = WarmSandBorder,
    outlineVariant = WarmSandBorder,
    error = SoftErrorText,
    onError = PureWhite,
    errorContainer = SoftErrorBg,
    onErrorContainer = SoftErrorText
)

val ClipFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipFlowLightColorScheme,
        typography = ClipFlowTypography,
        shapes = ClipFlowShapes,
        content = content
    )
}
