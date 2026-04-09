package com.quotamaster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.quotamaster.di.AppContainer
import com.quotamaster.ui.detail.ActivityDetailScreen
import com.quotamaster.ui.edit.CreateEditActivityScreen
import com.quotamaster.ui.home.HomeScreen
import com.quotamaster.ui.settings.SettingsScreen
import com.quotamaster.viewmodel.ActivityDetailViewModel
import com.quotamaster.viewmodel.HomeViewModel
import com.quotamaster.viewmodel.SettingsViewModel

private object Routes {
    const val HOME        = "home"
    const val DETAIL      = "detail/{activityId}"
    const val CREATE_EDIT = "create_edit?activityId={activityId}"
    const val SETTINGS    = "settings"

    fun detail(activityId: Long) = "detail/$activityId"
    fun edit(activityId: Long) = "create_edit?activityId=$activityId"
    fun create() = "create_edit?activityId=-1"
}

@Composable
fun AppNavigation(container: AppContainer) {
    val nav = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel(factory = container.homeViewModelFactory)

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        composable(Routes.HOME) {
            HomeScreen(
                viewModel          = homeViewModel,
                onActivityClick    = { activityId -> nav.navigate(Routes.detail(activityId)) },
                onCreateClick      = { nav.navigate(Routes.create()) },
                onSettingsClick    = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType })
        ) { backStack ->
            val activityId = backStack.arguments?.getLong("activityId") ?: return@composable
            val detailViewModel: ActivityDetailViewModel = viewModel(
                factory = container.detailViewModelFactory(activityId)
            )
            ActivityDetailScreen(
                viewModel      = detailViewModel,
                onNavigateBack = { nav.popBackStack() },
                onEditClick    = { nav.navigate(Routes.edit(activityId)) }
            )
        }

        composable(
            route = Routes.CREATE_EDIT,
            arguments = listOf(navArgument("activityId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStack ->
            val activityId = backStack.arguments?.getLong("activityId") ?: -1L
            CreateEditActivityScreen(
                activityId     = activityId,
                container      = container,
                onNavigateBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = container.settingsViewModelFactory
            )
            SettingsScreen(
                viewModel      = settingsViewModel,
                onNavigateBack = { nav.popBackStack() }
            )
        }
    }
}