package com.mergeseven.game.ui.feel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun ComboBanner(
    visible: Boolean,
    chainLength: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (chainLength <= 1) return
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduceMotion) {
                fadeIn()
            } else {
                fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f))
            },
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GameColors.CoinGold.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "COMBO x$chainLength",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = GameColors.WoodDark,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}
