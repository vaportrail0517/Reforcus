package com.example.refocus.feature.onboarding

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.refocus.permissions.PermissionHelper

@Composable
fun EntryScreen(
    onNeedFullOnboarding: () -> Unit,
    onNeedPermissionFix: () -> Unit,
    onAllReady: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        val hasUsage = PermissionHelper.hasUsageAccess(context)
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasNotif = PermissionHelper.hasNotificationPermission(context)
        val allGranted = hasUsage && hasOverlay && hasNotif

        val completed = OnboardingState.isCompleted(context)

        if (!completed) {
            onNeedFullOnboarding()
        } else {
            if (allGranted) {
                onAllReady()
            } else {
                onNeedPermissionFix()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
