package com.mergeseven.game.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mergeseven.game.R
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.ui.theme.GameColors

/**
 * Shared Merge Seven icons (DESIGN_SYSTEM icon_* drawables).
 */
object GameIcons {
    @DrawableRes val Coin = R.drawable.icon_coin
    @DrawableRes val Star = R.drawable.icon_star
    @DrawableRes val Pause = R.drawable.icon_pause
    @DrawableRes val Play = R.drawable.icon_play
    @DrawableRes val Lock = R.drawable.icon_lock
    @DrawableRes val Check = R.drawable.icon_check
    @DrawableRes val Back = R.drawable.icon_back
    @DrawableRes val Logo = R.drawable.logo_merge_seven
    @DrawableRes val WoodBackground = R.drawable.bg_wood_main

    @DrawableRes
    fun booster(type: BoosterType): Int = when (type) {
        BoosterType.UNDO -> R.drawable.icon_booster_undo
        BoosterType.SWAP -> R.drawable.icon_booster_swap
        BoosterType.REMOVE -> R.drawable.icon_booster_remove
        BoosterType.RANDOMIZE -> R.drawable.icon_booster_randomize
        BoosterType.CONTINUE -> R.drawable.icon_booster_continue
        BoosterType.HAMMER -> R.drawable.icon_booster_hammer
        BoosterType.VALUE_UP -> R.drawable.icon_booster_value_up
        BoosterType.MAGNET -> R.drawable.icon_booster_magnet
        BoosterType.TIME_FREEZE -> R.drawable.icon_booster_time_freeze
    }
}

@Composable
fun GameIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 24.dp
) {
    if (tint != null) {
        Icon(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint
        )
    } else {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            colorFilter = null
        )
    }
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = 18.dp) {
    GameIcon(
        resId = GameIcons.Coin,
        contentDescription = "Coins",
        modifier = modifier,
        size = size
    )
}

@Composable
fun StarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = GameColors.CoinGold
) {
    GameIcon(
        resId = GameIcons.Star,
        contentDescription = "Stars",
        modifier = modifier,
        tint = tint,
        size = size
    )
}
