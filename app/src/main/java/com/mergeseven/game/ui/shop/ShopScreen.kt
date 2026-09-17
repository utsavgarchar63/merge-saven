package com.mergeseven.game.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun ShopScreen(
    viewModel: ShopViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onCosmeticsClick: () -> Unit = {}
) {
    val coins by viewModel.coins.collectAsStateWithLifecycle()
    val af5Enabled by viewModel.af5Enabled.collectAsStateWithLifecycle()
    val af9Enabled by viewModel.af9Enabled.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val remoteOffer by viewModel.remoteOffer.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val removeAdsOwned by viewModel.removeAdsOwned.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity

    LaunchedEffect(Unit) { viewModel.onOpened() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text(stringResource(R.string.back), color = GameColors.TextWhite)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.shop_coins_balance, coins),
                color = GameColors.CoinGold,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.shop_title).uppercase(),
            color = GameColors.TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = if (af9Enabled) {
                if (removeAdsOwned) stringResource(R.string.shop_ad_free)
                else stringResource(R.string.shop_subtitle_af9)
            } else {
                stringResource(R.string.shop_subtitle_af9_off)
            },
            color = GameColors.TextWhite.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        status?.let {
            Text(text = it, color = GameColors.CoinGold, modifier = Modifier.padding(bottom = 8.dp))
        }

        /* if (af5Enabled) {
            OutlinedButton(
                onClick = onCosmeticsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("COSMETICS", color = GameColors.CoinGold)
            }
        } */

        if (af9Enabled) {
            OutlinedButton(
                onClick = { viewModel.restore() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("RESTORE PURCHASES", color = GameColors.TextWhite)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            remoteOffer?.let { offer ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GameColors.CoinGold.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(offer.title, color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
                            Text(offer.subtitle, color = GameColors.TextWhite.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { activity?.let { viewModel.buy(it, offer.productId) } },
                                enabled = activity != null && af9Enabled,
                                colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                            ) {
                                Text("CLAIM OFFER", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            items(offers) { offer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = offer.title,
                            color = GameColors.TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = offer.subtitle,
                            color = GameColors.TextWhite.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (!offer.comingSoon && activity != null) {
                                    viewModel.buy(activity, offer.id)
                                }
                            },
                            enabled = !offer.comingSoon && activity != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GameColors.CoinGold,
                                disabledContainerColor = GameColors.WoodLight.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (offer.comingSoon) "COMING SOON" else offer.priceLabel.ifBlank { "BUY" },
                                color = if (offer.comingSoon) Color.Black.copy(alpha = 0.5f) else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        if (activity != null) viewModel.watchEarnAd(activity)
                        else viewModel.grantStubReward()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (af9Enabled) {
                            "WATCH AD (+${com.mergeseven.game.core.Constants.REWARDED_COIN_GRANT} coins)"
                        } else {
                            "FREE STUB REWARD (+100 coins, +1 Undo)"
                        },
                        color = GameColors.CoinGold
                    )
                }
            }
        }
    }
}
