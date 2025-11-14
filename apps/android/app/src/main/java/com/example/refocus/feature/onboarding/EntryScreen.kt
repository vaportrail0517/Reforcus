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
    onNeedOnboarding: () -> Unit,
    onAllReady: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 起動直後に一度だけ権限をチェック
    LaunchedEffect(Unit) {
        val hasUsage = PermissionHelper.hasUsageAccess(context)
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasNotif = PermissionHelper.hasNotificationPermission(context)

        if (hasUsage && hasOverlay && hasNotif) {
            // すでに全部OK → ホームへ
            onAllReady()
        } else {
            // 足りない権限がある → オンボーディングへ
            onNeedOnboarding()
        }
    }

    // ほんの一瞬だけ出る読み込みUI（気になるならロゴなどに差し替え可）
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
