package com.mergeseven.game.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mergeseven.game.ui.game.GameScreen
import com.mergeseven.game.ui.home.HomeScreen

/**
 * Navigation routes for the app.
 * See Master Plan Section 47.
 */
object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val LEVELS = "levels"
    const val DAILY = "daily"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
}

/**
 * App-level navigation graph.
 * Navigation events should never create duplicate game sessions.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlayClick = {
                    navController.navigate(Routes.GAME) {
                        // Prevent multiple game instances on the back stack
                        launchSingleTop = true
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
                onShopClick = {
                    navController.navigate(Routes.SHOP) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.GAME) {
            GameScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.LEVELS) {
            // TODO: LevelsScreen
        }

        composable(Routes.DAILY) {
            // TODO: DailyScreen
        }

        composable(Routes.SHOP) {
            // TODO: ShopScreen
        }

        composable(Routes.SETTINGS) {
            // TODO: SettingsScreen
        }
    }
}
