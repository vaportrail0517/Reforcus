package com.example.refocus.feature.onboarding

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.refocus.permissions.PermissionHelper

@Composable
fun OnboardingScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsage by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(false) }

    // 通知許可リクエスト用ランチャー
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotif = granted
    }

    // 初回 & 以降の共通更新関数
    fun refreshPermissions() {
        hasUsage = PermissionHelper.hasUsageAccess(context)
        hasOverlay = PermissionHelper.hasOverlayPermission(context)
        hasNotif = PermissionHelper.hasNotificationPermission(context)
    }

    // 初回表示時に一度チェック
    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    // 画面に戻ってきた（ON_RESUME）ときにもチェック
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 3つ揃ったら自動的に次へ進めたい場合
    LaunchedEffect(hasUsage, hasOverlay, hasNotif) {
        if (hasUsage && hasOverlay && hasNotif) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("初期設定：必要な権限を有効にしてください")

        PermissionRow(
            title = "使用状況へのアクセス",
            description = "どのアプリが表示されているかを知るために必要です。",
            checked = hasUsage,
            onClick = {
                activity?.let { PermissionHelper.openUsageAccessSettings(it) }
            }
        )

        PermissionRow(
            title = "他のアプリの上に重ねて表示",
            description = "タイマーを他のアプリの上に表示するために必要です。",
            checked = hasOverlay,
            onClick = {
                activity?.let { PermissionHelper.openOverlaySettings(it) }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = "通知",
                description = "休憩の提案などを通知でお知らせします。",
                checked = hasNotif,
                onClick = {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 手動で進めたい場合はボタンでもOK
        // Button(
        //   enabled = hasUsage && hasOverlay && hasNotif,
        //   onClick = onAllPermissionsGranted
        // ) { Text("次へ") }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(title)
            Text(description)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { if (!checked) onClick() }
        )
    }
}
