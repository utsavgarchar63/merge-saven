package com.mergeseven.game.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mergeseven.game.R
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // ─── Top Header Bar ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(GameColors.WoodMid, CircleShape)
                ) {
                    GameIcon(
                        resId = GameIcons.Back,
                        contentDescription = stringResource(R.string.back),
                        tint = GameColors.TextWhite,
                        size = 24.dp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "PRIVACY POLICY",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )
            }

            // ─── Privacy Policy Content ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Privacy Policy for Merge Seven",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.CoinGold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Last updated: September 2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = GameColors.TextWhite.copy(alpha = 0.6f)
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        // Section 1
                        PolicySection(
                            title = "1. No Personal Data Collection",
                            body = "Merge Seven does NOT collect, store, transmit, or sell any personal data such as your name, email address, phone number, location, or contacts. You can play Merge Seven with complete peace of mind."
                        )

                        // Section 2
                        PolicySection(
                            title = "2. Advertisements (Google AdMob)",
                            body = "Merge Seven integrates Google AdMob to show non-intrusive advertisements (banner, interstitial, and optional rewarded ads). AdMob may use anonymized device identifiers to serve relevant ads and prevent fraud in full compliance with Google's Advertising Policies and applicable privacy laws."
                        )

                        // Section 3
                        PolicySection(
                            title = "3. Local Data Storage",
                            body = "All your game progress, high scores, collected coins, stars, and preferences are stored locally on your device. You can reset or clear your game data at any time directly from the Settings menu."
                        )

                        // Section 4
                        PolicySection(
                            title = "4. Optional Cloud Backup",
                            body = "If you choose to sign in with Google Play Games, your save game progress is backed up securely to your personal Google account. We do not access or share your Google account data."
                        )

                        // Section 5
                        PolicySection(
                            title = "5. Children's Privacy",
                            body = "Merge Seven is a family-friendly hexagon puzzle game. We do not knowingly collect any data from children or any users."
                        )

                        // Section 6
                        PolicySection(
                            title = "6. Contact Us",
                            body = "If you have any questions or feedback regarding our privacy policy, feel free to contact our support team at support@mergeseven.com."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = GameColors.CoinGold,
            fontSize = 14.sp
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = GameColors.TextWhite.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
