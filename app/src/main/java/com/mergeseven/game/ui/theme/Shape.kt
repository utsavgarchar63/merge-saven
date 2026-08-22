package com.mergeseven.game.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Game corner radius and shape tokens.
 * See Phase 2 Design System.
 */
val GameShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object GameCornerRadius {
    val small = 4.dp
    val medium = 8.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val pill = 50.dp
}
