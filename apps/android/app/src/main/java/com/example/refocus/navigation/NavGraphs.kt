package com.example.refocus.navigation

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.refocus.feature.onboarding.EntryScreen
import com.example.refocus.feature.onboarding.OnboardingIntroScreen
import com.example.refocus.feature.onboarding.PermissionFlowScreen
import com.example.refocus.feature.onboarding.OnboardingReadyScreen
import com.example.refocus.feature.onboarding.OnboardingFinishScreen
import com.example.refocus.feature.appselect.AppSelectScreen
import com.example.refocus.feature.home.HomeScreen
import com.example.refocus.feature.overlay.OverlayService
import com.example.refocus.feature.overlay.startOverlayService

object Destinations {
    const val ENTRY            = "entry"
    const val ONBOARDING_INTRO  = "onboarding_intro"
    const val PERMISSION_FLOW   = "permission_flow"
    const val ONBOARDING_READY  = "onboarding_ready"
    const val APP_SELECT        = "app_select"
    const val ONBOARDING_FINISH = "onboarding_finish"
    // 将来: const val HOME = "home" など
    const val HOME = "home"
}

@Composable
fun RefocusNavHost(
    // 将来: onExitApp や onOpenHome など渡したくなったらここに引数追加
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Destinations.ENTRY
    ) {
        composable(Destinations.ENTRY) {
            EntryScreen(
                onNeedOnboarding = {
                    navController.navigate(Destinations.ONBOARDING_INTRO) {
                        popUpTo(Destinations.ENTRY) { inclusive = true }
                    }
                },
                onAllReady = {
                    Log.d("NavGraphs", "ENTRY onAllReady → startOverlayService")
                    context.startOverlayService()
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.ENTRY) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.ONBOARDING_INTRO) {
            OnboardingIntroScreen(
                onStartSetup = {
                    navController.navigate(Destinations.PERMISSION_FLOW)
                }
            )
        }

        composable(Destinations.PERMISSION_FLOW) {
            PermissionFlowScreen(
                onFlowFinished = {
                    navController.navigate(Destinations.ONBOARDING_READY) {
                        popUpTo(Destinations.ONBOARDING_INTRO) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.ONBOARDING_READY) {
            OnboardingReadyScreen(
                onSelectApps = {
                    navController.navigate(Destinations.APP_SELECT)
                }
            )
        }

        composable(Destinations.APP_SELECT) {
            AppSelectScreen(
                onFinished = {
                    navController.navigate(Destinations.ONBOARDING_FINISH) {
                        popUpTo(Destinations.ONBOARDING_READY) { inclusive = false }
                    }
                }
            )
        }

        composable(Destinations.ONBOARDING_FINISH) {
            val context = LocalContext.current
            val activity = context as? Activity
            OnboardingFinishScreen(
                onCloseApp = {
                    // 仮
                    activity?.finishAffinity()
                },
                onOpenApp = {
                    Log.d("NavGraphs", "ONBOARDING_FINISH onOpenApp → startOverlayService")
                    context.startOverlayService()
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.APP_SELECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.HOME) {
            HomeScreen()
        }
    }
}

