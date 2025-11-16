package com.example.refocus.feature.home

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.refocus.permissions.PermissionHelper
import com.example.refocus.ui.components.SectionTitle
import com.example.refocus.ui.components.SectionCard
import com.example.refocus.ui.components.SettingRow
import com.example.refocus.feature.history.SessionHistoryScreen

enum class HomeTab {
    Suggestions,
    Stats,
    Settings
}

@Composable
fun HomeScreen(
    onOpenAppSelect: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Settings) }

    Scaffold(
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                HomeTab.Suggestions -> SuggestionsTab()
                HomeTab.Stats       -> StatsTab()
                HomeTab.Settings    -> SettingsTab(onOpenAppSelect = onOpenAppSelect)
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == HomeTab.Suggestions,
            onClick = { onTabSelected(HomeTab.Suggestions) },
            icon = { Icon(Icons.Filled.Lightbulb, contentDescription = "提案") },
            label = { Text("提案") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.Stats,
            onClick = { onTabSelected(HomeTab.Stats) },
            icon = { Icon(Icons.Filled.Insights, contentDescription = "統計") },
            label = { Text("統計") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.Settings,
            onClick = { onTabSelected(HomeTab.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "設定") },
            label = { Text("設定") }
        )
    }
}

@Composable
private fun SuggestionsTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("提案タブ（将来ここに提案UIを実装）")
    }
}

@Composable
private fun StatsTab() {
    SessionHistoryScreen()
}

@Composable
private fun SettingsTab(
    onOpenAppSelect: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var usageGranted by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var overlayGranted by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var notificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }

    // 画面復帰時に権限状態を更新
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = PermissionHelper.hasUsageAccess(context)
                overlayGranted = PermissionHelper.hasOverlayPermission(context)
                notificationGranted = PermissionHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle("権限")
        SectionCard {
            PermissionRow(
                title = "使用状況へのアクセス",
                description = "連続使用時間を計測するために必要です。",
                isGranted = usageGranted,
                onClick = {
                    activity?.let { PermissionHelper.openUsageAccessSettings(it) }
                }
            )
            PermissionRow(
                title = "他のアプリの上に表示",
                description = "タイマーを他のアプリの上に表示するために必要です。",
                isGranted = overlayGranted,
                onClick = {
                    activity?.let { PermissionHelper.openOverlaySettings(it) }
                }
            )
            PermissionRow(
                title = "通知",
                description = "やることの提案やお知らせに利用します。",
                isGranted = notificationGranted,
                onClick = {
                    activity?.let { PermissionHelper.openNotificationSettings(it) }
                }
            )
        }

        SectionTitle("対象アプリ")
        SectionCard {
            SettingRow(
                title = "対象アプリを設定",
                subtitle = "時間を計測したいアプリを選択します。",
                onClick = onOpenAppSelect
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isGranted,
            onCheckedChange = null,
            enabled = true,
        )
    }
}
