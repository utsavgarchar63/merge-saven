package com.mergeseven.game.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.ads.BannerAdHost
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.meta.XpCurve
import com.mergeseven.game.ui.components.CoinIcon
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.components.StarIcon
import com.mergeseven.game.ui.theme.GameColors
import kotlin.math.sin

/**
 * Home Screen with optional AF2 mode selector.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlayClick: () -> Unit = {},
    onContinueClick: (Int) -> Unit = {},
    onContinueModeClick: (modeId: String, levelId: Int) -> Unit = { _, level -> onContinueClick(level) },
    onModeClick: (String) -> Unit = {},
    onLevelsClick: () -> Unit = {},
    onDailyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onCosmeticsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onLeaderboardsClick: () -> Unit = {},
    onTournamentPlay: (Long) -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val resumableGame by viewModel.resumableGame.collectAsStateWithLifecycle()
    val af2Enabled by viewModel.af2Enabled.collectAsStateWithLifecycle()
    val af5Enabled by viewModel.af5Enabled.collectAsStateWithLifecycle()
    val af8Enabled by viewModel.af8Enabled.collectAsStateWithLifecycle()
    val af9Enabled by viewModel.af9Enabled.collectAsStateWithLifecycle()
    val af10Enabled by viewModel.af10Enabled.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsStateWithLifecycle()
    val modeCards by viewModel.modeCards.collectAsStateWithLifecycle()
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val remoteOffer by viewModel.remoteOffer.collectAsStateWithLifecycle()
    val stipendMessage by viewModel.stipendMessage.collectAsStateWithLifecycle()

    val animateHome = af10Enabled && !reduceMotion
    val infinite = rememberInfiniteTransition(label = "homeFeel")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgDrift"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tileShimmer"
    )

    LaunchedEffect(Unit) {
        viewModel.refreshResumable()
        viewModel.refreshTournament()
    }

// NotificationSoftAskHost()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(GameIcons.WoodBackground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (animateHome) {
                        translationX = (drift - 0.5f) * 24f
                        translationY = (drift - 0.5f) * 16f
                        scaleX = 1.06f
                        scaleY = 1.06f
                    }
                },
            contentScale = ContentScale.Crop
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GameColors.WoodDark.copy(alpha = 0.75f - if (animateHome) drift * 0.08f else 0f),
                            GameColors.WoodMid.copy(alpha = 0.55f),
                            GameColors.WoodDark.copy(alpha = 0.8f)
                        )
                    )
                )
                .statusBarsPadding()
        ) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                if (af9Enabled) {
                    BannerAdHost()
                }
                stipendMessage?.let {
                    Text(it, color = GameColors.CoinGold, fontSize = 12.sp)
                }

                // ─── Top Bar Header: Centered Coins & Stars Only ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center: Prominent, Centered Stars & Coins Header Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = GameColors.WoodDark.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    GameColors.CoinGold.copy(alpha = 0.8f),
                                    GameColors.TileGold.copy(alpha = 0.95f),
                                    GameColors.CoinGold.copy(alpha = 0.8f)
                                )
                            )
                        ),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Total Stars
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StarIcon(size = 22.dp)
                                Text(
                                    text = "${userProfile.totalStars}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    ),
                                    color = GameColors.TextWhite
                                )
                            }

                            // Glowing Divider Dot
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(GameColors.CoinGold.copy(alpha = 0.7f))
                            )

                            // Coins
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CoinIcon(size = 22.dp)
                                Text(
                                    text = "${userProfile.coins}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    ),
                                    color = GameColors.CoinGold
                                )
                            }
                        }
                    }
                }

                HomeTitleHeaderBanner(animateHome = animateHome, shimmer = shimmer)

                // ─── Vertically Centered Main 4 Buttons ───
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val resumable = resumableGame
                    if (resumable != null) {
                        val label = if (resumable.modeId == ModeIds.CAMPAIGN) {
                            "CONTINUE • LEVEL ${resumable.level}"
                        } else {
                            "CONTINUE • ${resumable.modeId.replace('_', ' ').uppercase()}"
                        }
                        HomeButton(
                            text = label,
                            onClick = {
                                onContinueModeClick(resumable.modeId, resumable.level)
                            },
                            primary = true,
                            iconRes = GameIcons.Play
                        )
                    } else {
                        HomeButton(
                            text = "PLAY NOW",
                            onClick = onPlayClick,
                            primary = true,
                            iconRes = GameIcons.Play
                        )
                    }

                    HomeButton(text = "LEVELS MAP", onClick = onLevelsClick, iconRes = GameIcons.Levels)
                    HomeButton(text = "DAILY REWARDS", onClick = onDailyClick, iconRes = GameIcons.Daily)
                    HomeButton(text = stringResource(com.mergeseven.game.R.string.home_settings).uppercase(), onClick = onSettingsClick, iconRes = GameIcons.Settings)
                }
            }
        }
    }
}

@Composable
private fun IdleTileShimmerRow(shimmer: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .height(36.dp)
    ) {
        val colors = listOf(
            GameColors.TileBlue,
            GameColors.TileGreen,
            GameColors.TileGold,
            GameColors.TilePurple,
            GameColors.TileTeal
        )
        val spacing = size.width / colors.size
        colors.forEachIndexed { index, color ->
            val cx = spacing * (index + 0.5f)
            val cy = size.height / 2f
            val pulse = 0.75f + 0.25f * sin((shimmer + index * 0.2f) * Math.PI.toFloat())
            drawCircle(
                color = color.copy(alpha = 0.35f + 0.45f * shimmer),
                radius = 10f * pulse,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.2f * shimmer),
                radius = 4f,
                center = Offset(cx - 3f, cy - 3f)
            )
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    iconRes: Int? = null
) {
    var iconRotationDegrees by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val animatedRotationDegrees by animateFloatAsState(
        targetValue = iconRotationDegrees,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "homeBtnIconRotate"
    )

    Button(
        onClick = {
            iconRotationDegrees += 360f
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (primary) 58.dp else 48.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) GameColors.CoinGold
            else GameColors.WoodLight.copy(alpha = 0.35f),
            contentColor = if (primary) Color.Black
            else GameColors.TextWhite
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconRes != null) {
                GameIcon(
                    resId = iconRes,
                    contentDescription = null,
                    size = if (primary) 22.dp else 18.dp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = animatedRotationDegrees
                    }
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (primary) 18.sp else 15.sp
                )
            )
        }
    }
}

@Composable
private fun HomeTitleHeaderBanner(
    animateHome: Boolean,
    shimmer: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(110.dp)
            ) {
                // Radial gold halo aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GameColors.CoinGold.copy(alpha = 0.35f + shimmer * 0.15f),
                            GameColors.TilePurple.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    radius = size.width * 0.45f
                )

                // Render decorative glowing hexagon game tile graphics
                val path = androidx.compose.ui.graphics.Path()
                val hexRadius = 24f
                val centers = listOf(
                    Offset(size.width * 0.18f, size.height * 0.4f) to GameColors.TileBlue,
                    Offset(size.width * 0.82f, size.height * 0.4f) to GameColors.TileGreen,
                    Offset(size.width * 0.30f, size.height * 0.25f) to GameColors.TileGold,
                    Offset(size.width * 0.70f, size.height * 0.25f) to GameColors.TilePurple
                )

                centers.forEach { (center, color) ->
                    path.reset()
                    for (i in 0..5) {
                        val angleRad = (Math.PI / 3 * i - Math.PI / 6).toFloat()
                        val x = center.x + hexRadius * kotlin.math.cos(angleRad)
                        val y = center.y + hexRadius * kotlin.math.sin(angleRad)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path = path, color = color.copy(alpha = 0.65f))
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Central Glowing Number 7 Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GameColors.WoodDark.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(GameColors.CoinGold, GameColors.TileGold, GameColors.CoinGold)
                        )
                    ),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💎 MERGE SEVEN 💎",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                letterSpacing = 2.sp
                            ),
                            color = GameColors.CoinGold
                        )
                    }
                }
            }
        }

        if (animateHome) {
            Spacer(modifier = Modifier.height(6.dp))
            IdleTileShimmerRow(shimmer = shimmer)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GameColors.WoodLight.copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GameColors.CoinGold.copy(alpha = 0.4f))
        ) {
            Text(
                text = "HEXAGON MERGE PUZZLE • v1.4.0",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                ),
                color = GameColors.CoinGold.copy(alpha = 0.95f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

