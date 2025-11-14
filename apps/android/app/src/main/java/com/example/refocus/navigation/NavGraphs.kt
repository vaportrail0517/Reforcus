package com.example.refocus.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.refocus.feature.onboarding.OnboardingScreen
import com.example.refocus.feature.appselect.AppSelectScreen

object Destinations {
    const val ONBOARDING = "onboarding"
    const val APP_SELECT = "app_select"
    const val DONE = "done"
}

@Composable
fun RefocusNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.ONBOARDING
    ) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onAllPermissionsGranted = {
                    navController.navigate(Destinations.APP_SELECT) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.APP_SELECT) {
            AppSelectScreen(
                onFinished = {
                    // 後で「ホーム画面」などへ遷移させる場所
                    navController.navigate(Destinations.DONE) {
                        popUpTo(Destinations.APP_SELECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.DONE) {
            Text("セットアップ完了（この画面は仮です）")
        }
    }
}
