package com.example.shufa.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.shufa.ui.select.SelectScreen
import com.example.shufa.ui.view.ViewScreen

object Routes {
    const val SELECT = "select"
    const val VIEW = "view/{postId}"

    fun view(postId: String) = "view/$postId"
}

@Composable
fun ShufaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SELECT
    ) {
        composable(Routes.SELECT) {
            SelectScreen(
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
