package com.example.shufa.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.shufa.ui.home.HomeScreen
import com.example.shufa.ui.view.ViewScreen

object Routes {
    const val HOME = "home"
    const val VIEW = "view/{postId}"

    fun view(postId: String) = "view/$postId"
}

@Composable
fun ShufaNavGraph(
    navController: NavHostController,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onPostClick = { postId ->
                    navController.navigate(Routes.view(postId))
                }
            )
        }

        composable(
            route = Routes.VIEW,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            ViewScreen(
                postId = postId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
