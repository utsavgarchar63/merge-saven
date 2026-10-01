package com.mergeseven.game.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mergeseven.game.R
import com.mergeseven.game.ui.profile.ProfileScreen
import com.mergeseven.game.ui.theme.GameColors
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.ui.achievements.AchievementsScreen
import com.mergeseven.game.ui.cosmetics.CosmeticsScreen
import com.mergeseven.game.ui.daily.DailyScreen
import com.mergeseven.game.ui.game.GameScreen
import com.mergeseven.game.ui.home.HomeScreen
import com.mergeseven.game.ui.leaderboard.LeaderboardScreen
import com.mergeseven.game.ui.levels.LevelsScreen
import com.mergeseven.game.ui.settings.SettingsScreen
import com.mergeseven.game.ui.shop.ShopScreen
import com.mergeseven.game.ui.stats.StatsScreen

/**
 * Navigation routes for the app.
 * See Master Plan Section 47 / AF2-08.
 */
object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val GAME = "game"
    const val LEVELS = "levels"
    const val DAILY = "daily"
    const val SETTINGS = "settings"
    const val SHOP = "shop"
    const val ACHIEVEMENTS = "achievements"
    const val COSMETICS = "cosmetics"
    const val STATS = "stats"
    const val LEADERBOARDS = "leaderboards"
    const val PLAY = "play/{mode}?levelId={levelId}&seed={seed}"

    fun play(
        mode: String,
        levelId: Int? = null,
        seed: Long? = null
    ): String {
        val lid = levelId ?: 1
        val s = seed ?: -1L
        return "play/$mode?levelId=$lid&seed=$s"
    }
}

/**
 * App-level navigation graph.
 * Navigation events should never create duplicate game sessions.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME
    val tabs = listOf(Triple(Routes.HOME, "Home", R.drawable.icon_nav_home),
        Triple(Routes.DAILY, "Challenges", R.drawable.icon_nav_challenges),
        Triple(Routes.SHOP, "Rewards", R.drawable.icon_nav_rewards),
        Triple(Routes.PROFILE, "Profile", R.drawable.icon_nav_profile))
    Scaffold(containerColor = GameColors.WoodDark, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (tabs.any { it.first == route }) NavigationBar(containerColor = GameColors.WoodDark) {
            tabs.forEach { (destination, label, icon) -> NavigationBarItem(selected = destination == route,
                onClick = { navController.navigate(destination) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true; restoreState = true
                } }, icon = { Icon(painterResource(icon), null, Modifier.size(24.dp)) }, label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = GameColors.CoinGold,
                    selectedTextColor = GameColors.CoinGold, indicatorColor = GameColors.WoodMid)) }
        }
    }) { padding ->
    NavHost(
        modifier = Modifier.padding(padding),
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlayClick = {
                    navController.navigate(Routes.LEVELS) {
                        launchSingleTop = true
                    }
                },
                onContinueClick = { levelId ->
                    navController.navigate(Routes.play(ModeIds.CAMPAIGN, levelId = levelId)) {
                        launchSingleTop = true
                    }
                },
                onContinueModeClick = { mode, levelId ->
                    navController.navigate(
                        Routes.play(
                            mode = mode,
                            levelId = if (mode == ModeIds.CAMPAIGN) levelId else 1
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
                onModeClick = { mode ->
                    when (mode) {
                        ModeIds.CAMPAIGN -> navController.navigate(Routes.LEVELS) {
                            launchSingleTop = true
                        }
                        ModeIds.DAILY -> navController.navigate(Routes.DAILY) {
                            launchSingleTop = true
                        }
                        else -> navController.navigate(Routes.play(mode)) {
                            launchSingleTop = true
                        }
                    }
                },
                onLevelsClick = {
                    navController.navigate(Routes.LEVELS) {
                        launchSingleTop = true
                    }
                },
                onDailyClick = {
                    navController.navigate(Routes.DAILY) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onShopClick = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                },
                onAchievementsClick = {
                    navController.navigate(Routes.ACHIEVEMENTS) {
                        launchSingleTop = true
                    }
                },
                onCosmeticsClick = {
                    navController.navigate(Routes.COSMETICS) {
                        launchSingleTop = true
                    }
                },
                onStatsClick = {
                    navController.navigate(Routes.STATS) {
                        launchSingleTop = true
                    }
                },
                onLeaderboardsClick = {
                    navController.navigate(Routes.LEADERBOARDS) {
                        launchSingleTop = true
                    }
                },
                onTournamentPlay = { seed ->
                    navController.navigate(Routes.play(ModeIds.WEEKLY, seed = seed)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Legacy campaign aliases — SavedStateHandle gets levelId, mode defaults to campaign.
        composable(Routes.GAME) {
            GameScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNavigateShop = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "${Routes.GAME}/{levelId}",
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) {
            GameScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNavigateShop = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.PLAY,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("levelId") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("seed") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            GameScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onNavigateShop = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.LEVELS) {
            LevelsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onStartLevel = { levelId ->
                    navController.navigate(Routes.play(ModeIds.CAMPAIGN, levelId = levelId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.DAILY) {
            DailyScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onStartWeekly = { navController.navigate(Routes.play(ModeIds.WEEKLY)) { launchSingleTop = true } },
                onSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onStartDailyChallenge = {
                    navController.navigate(Routes.play(ModeIds.DAILY)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onReplayTutorial = { navController.navigate(Routes.play(ModeIds.CAMPAIGN, 1)) { launchSingleTop = true } },
                onBackClick = {
                    navController.popBackStack()
                },
                onShopClick = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                },
                onAchievementsClick = {
                    navController.navigate(Routes.ACHIEVEMENTS) {
                        launchSingleTop = true
                    }
                },
                onCosmeticsClick = {
                    navController.navigate(Routes.COSMETICS) {
                        launchSingleTop = true
                    }
                },
                onStatsClick = {
                    navController.navigate(Routes.STATS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SHOP) {
            ShopScreen(
                onDailyClick = { navController.navigate(Routes.DAILY) { launchSingleTop = true } },
                onBackClick = {
                    navController.popBackStack()
                },
                onCosmeticsClick = {
                    navController.navigate(Routes.COSMETICS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(onAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onStats = { navController.navigate(Routes.STATS) }, onCosmetics = { navController.navigate(Routes.COSMETICS) },
                onLeaderboards = { navController.navigate(Routes.LEADERBOARDS) }, onSettings = { navController.navigate(Routes.SETTINGS) })
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.COSMETICS) {
            CosmeticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.LEADERBOARDS) {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
}
