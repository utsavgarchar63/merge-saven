package com.mergeseven.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cards share the page's scroll container; column count follows actual available space. */
@Composable
fun <T> ResponsiveCards(
    items: List<T>,
    minCardWidth: Dp = 260.dp,
    maxColumns: Int = 2,
    itemKey: (T) -> Any,
    content: @Composable (T) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 12.dp
        val readableWidth = minCardWidth * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val columns = ((maxWidth + gap) / (readableWidth + gap)).toInt().coerceIn(1, maxColumns)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { item ->
                        key(itemKey(item)) { Box(Modifier.weight(1f)) { content(item) } }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
