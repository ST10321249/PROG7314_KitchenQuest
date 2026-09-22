package com.example.kitchenquest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = KitchenOrange,
    onPrimary = KitchenSurface,

    primaryContainer = KitchenOrangeLight,
    onPrimaryContainer = KitchenText,

    secondary = KitchenGreen,
    onSecondary = KitchenSurface,

    secondaryContainer = KitchenGreenLight,
    onSecondaryContainer = KitchenText,

    background = KitchenBackground,
    onBackground = KitchenText,

    surface = KitchenSurface,
    onSurface = KitchenText,

    surfaceVariant = KitchenSurfaceMuted,
    onSurfaceVariant = KitchenTextSecondary,

    outline = KitchenBorder,
    outlineVariant = KitchenBorder,

    error = KitchenRed,
    errorContainer = KitchenRedLight
)

private val DarkColorScheme = darkColorScheme(
    primary = KitchenOrange,
    onPrimary = KitchenSurface,

    primaryContainer = KitchenOrangeDark,
    onPrimaryContainer = KitchenDarkText,

    secondary = KitchenGreen,

    background = KitchenDarkBackground,
    onBackground = KitchenDarkText,

    surface = KitchenDarkSurface,
    onSurface = KitchenDarkText,

    surfaceVariant = KitchenDarkSurfaceMuted,
    onSurfaceVariant = KitchenDarkTextSecondary,

    error = KitchenRed
)

private val KitchenQuestShapes = Shapes(
    small = RoundedCornerShape(KitchenQuestDimens.SmallCorner),
    medium = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
    large = RoundedCornerShape(KitchenQuestDimens.LargeCorner)
)

@Composable
fun KitchenQuestTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // The supplied Part 2 designs define a light visual system. Dark colours are
    // retained above for a future optional dark theme, but the prototype uses
    // the Figma-aligned light theme unless explicitly overridden.
    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KitchenQuestTypography,
        shapes = KitchenQuestShapes,
        content = content
    )
}
