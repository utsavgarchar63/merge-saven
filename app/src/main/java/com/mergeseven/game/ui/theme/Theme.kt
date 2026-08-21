package com.mergeseven.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Merge Seven Material3 theme.
 * Uses a dark color scheme with warm wood tones.
 */
private val MergeSevenColorScheme = darkColorScheme(
    primary = GameColors.WoodLight,
    onPrimary = GameColors.TextWhite,
    primaryContainer = GameColors.WoodMid,
    onPrimaryContainer = GameColors.TextWhite,

    secondary = GameColors.CoinGold,
    onSecondary = GameColors.TextDark,
    secondaryContainer = GameColors.TileGold,
    onSecondaryContainer = GameColors.TextDark,

    tertiary = GameColors.TileBlue,
    onTertiary = GameColors.TextWhite,

    background = GameColors.WoodDark,
    onBackground = GameColors.TextWhite,

    surface = GameColors.WoodMid,
    onSurface = GameColors.TextWhite,
    surfaceVariant = GameColors.WoodLight,
    onSurfaceVariant = GameColors.TextWhite,

    error = GameColors.Error,
    onError = GameColors.TextWhite
)

@Composable
fun MergeSevenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MergeSevenColorScheme,
        typography = MergeSevenTypography,
        content = content
    )
}
