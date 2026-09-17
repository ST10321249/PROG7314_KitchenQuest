package com.example.kitchenquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

    background = KitchenBackground,
    onBackground = KitchenText,

    surface = KitchenSurface,
    onSurface = KitchenText,

    surfaceVariant = KitchenSurfaceMuted,
    onSurfaceVariant = KitchenTextSecondary,

    outline = KitchenBorder,

    error = KitchenRed
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

@Composable
fun KitchenQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KitchenQuestTypography,
        content = content
    )
}