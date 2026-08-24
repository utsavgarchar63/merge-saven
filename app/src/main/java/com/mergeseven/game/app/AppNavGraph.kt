package com.mergeseven.game.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mergeseven.game.ui.daily.DailyScreen
import com.mergeseven.game.ui.game.GameScreen
import com.mergeseven.game.ui.home.HomeScreen
import com.mergeseven.game.ui.levels.LevelsScreen
import com.mergeseven.game.ui.settings.SettingsScreen

/**
 * Navigation routes for the app.
 * See Master Plan Section 47.
 */
object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val LEVELS = "levels"
    const val DAILY = "daily"
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
                    navController.navigate(Routes.LEVELS) {
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

        composable(
            route = "${Routes.GAME}/{levelId}",
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) {
            GameScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.LEVELS) {
            LevelsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onStartLevel = { levelId ->
                    navController.navigate("${Routes.GAME}/$levelId") {
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
                onStartDailyChallenge = {
                    navController.navigate(Routes.GAME) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
