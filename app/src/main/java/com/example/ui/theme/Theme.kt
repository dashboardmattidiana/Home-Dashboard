package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MidCenturyDarkScheme = darkColorScheme(
    primary = TerracottaAccent,
    onPrimary = WarmCream,
    secondary = OliveAccent,
    onSecondary = EspressoBg,
    tertiary = OchreAccent,
    onTertiary = EspressoBg,
    background = EspressoBg,
    onBackground = WarmCream,
    surface = EspressoCard,
    onSurface = WarmCream,
    surfaceVariant = WalnutCard,
    onSurfaceVariant = WarmTaupe,
    outline = ContourSubtle,
    outlineVariant = WalnutDeep
)

private val MidCenturyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun HeimboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MidCenturyDarkScheme,
        typography = HeimboardTypography,
        shapes = MidCenturyShapes,
        content = content
    )
}
