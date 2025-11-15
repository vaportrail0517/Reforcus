package com.example.refocus.feature.onboarding

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.refocus.permissions.PermissionHelper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PermissionType {
    UsageAccess,
    Overlay,
    Notifications
}

enum class PermissionUIMode {
    TwoStepWithExplain,
    DirectGrant
}

data class PermissionStep(
    val type: PermissionType,
    val uiMode: PermissionUIMode
)

enum class PermissionPage {
    IntroOrSingle,
    Explain
}

@Composable
fun PermissionFlowScreen(
    onFlowFinished: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // 通知権限リクエストランチャー
    var requestNotificationPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // POST_NOTIFICATIONS はダイアログ内で完結するので、
        // 結果を見て次のステップに進む
        if (granted) {
            requestNotificationPermission?.invoke()
        }
    }

    // 権限定義（ここでは UI 文言は持たせず、タイプとモードだけ持つ）
    val steps = remember {
        buildList {
            add(
                PermissionStep(
                    type = PermissionType.UsageAccess,
                    uiMode = PermissionUIMode.TwoStepWithExplain
                )
            )
            add(
                PermissionStep(
                    type = PermissionType.Overlay,
                    uiMode = PermissionUIMode.TwoStepWithExplain
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionStep(
                        type = PermissionType.Notifications,
                        uiMode = PermissionUIMode.DirectGrant
                    )
                )
            }
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(PermissionPage.IntroOrSingle) }

    val currentStep = steps.getOrNull(currentIndex)

    // すべてすでに許可済みなら即終了
    LaunchedEffect(Unit) {
        if (allPermissionsGranted(context)) {
            onFlowFinished()
        }
    }

    // index が範囲外 → フロー完了
    LaunchedEffect(currentIndex, steps.size) {
        if (currentIndex >= steps.size) {
            onFlowFinished()
        }
    }

    if (currentStep == null || activity == null) {
        return
    }

    // 設定アプリから戻ってきた時（ON_RESUME）に権限を再チェック
    DisposableEffect(lifecycleOwner, currentIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val step = steps.getOrNull(currentIndex)
                if (step != null && isGranted(context, step.type)) {
                    moveToNextStep(
                        stepsSize = steps.size,
                        currentIndex = currentIndex,
                        setIndex = { currentIndex = it },
                        setPage = { currentPage = it }
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ここから UI 部分（権限ごとにページを分割）

    when (currentStep.type) {
        PermissionType.UsageAccess -> {
            when (currentPage) {
                PermissionPage.IntroOrSingle -> {
                    UsageAccessIntroPage(
                        onNext = { currentPage = PermissionPage.Explain }
                    )
                }
                PermissionPage.Explain -> {
                    UsageAccessExplainPage(
                        onRequestPermission = {
                            PermissionHelper.openUsageAccessSettings(activity)
                        }
                    )
                }
            }
        }

        PermissionType.Overlay -> {
            when (currentPage) {
                PermissionPage.IntroOrSingle -> {
                    OverlayIntroPage(
                        onNext = { currentPage = PermissionPage.Explain }
                    )
                }
                PermissionPage.Explain -> {
                    OverlayExplainPage(
                        onRequestPermission = {
                            PermissionHelper.openOverlaySettings(activity)
                        }
                    )
                }
            }
        }

        PermissionType.Notifications -> {
            // 通知は DirectGrant 想定（いきなり許可ボタン）
            NotificationPermissionPage(
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // ランチャーに「次のステップへ進む処理」を渡しておく
                        requestNotificationPermission = {
                            moveToNextStep(
                                stepsSize = steps.size,
                                currentIndex = currentIndex,
                                setIndex = { currentIndex = it },
                                setPage = { currentPage = it }
                            )
                        }
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // 古い端末では実質常に許可扱い
                        moveToNextStep(
                            stepsSize = steps.size,
                            currentIndex = currentIndex,
                            setIndex = { currentIndex = it },
                            setPage = { currentPage = it }
                        )
                    }
                }
            )
        }
    }
}

/* ---------- 各権限ごとのページ（ここを自由にデザインできる） ---------- */

@Composable
private fun UsageAccessIntroPage(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("使用状況へのアクセス")
        Text("どのアプリをどれだけ連続して使っているかを計測するために必要です。")

        // TODO: ここに画像や図解などを自由に追加してOK
        // Image(painterResource(id = R.drawable.usage_intro), contentDescription = null)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("続行")
        }
    }
}

@Composable
private fun UsageAccessExplainPage(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("使用状況へのアクセスを有効にしましょう")

        // TODO: 実際の設定画面のスクリーンショットなどをここに追加可能
        Text("1. 「許可」を押すと設定アプリが開きます。")
        Text("2. アプリ一覧から「Refocus」を探してタップします。")
        Text("3. 「使用状況へのアクセス」をオンにします。")

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("許可")
        }
    }
}

@Composable
private fun OverlayIntroPage(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("他のアプリの上に重ねて表示")
        Text("タイマーを他のアプリの上に小さく表示するために必要です。")

        // TODO: ここにもオーバーレイのイメージ図などを追加できる

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("続行")
        }
    }
}

@Composable
private fun OverlayExplainPage(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("他のアプリの上に表示を許可しましょう")

        Text("1. 「許可」を押すと設定アプリが開きます。")
        Text("2. 一覧から「Refocus」を選びます。")
        Text("3. 「他のアプリの上に表示」をオンにします。")

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("許可")
        }
    }
}

@Composable
private fun NotificationPermissionPage(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("通知")
        Text("やることの提案などを行うために通知を使います。")

        // TODO: 通知の例（モック画面）などを画像で入れてもOK

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("許可")
        }
    }
}

/* ---------- ヘルパー ---------- */

private fun allPermissionsGranted(context: android.content.Context): Boolean {
    return PermissionHelper.hasUsageAccess(context) &&
            PermissionHelper.hasOverlayPermission(context) &&
            PermissionHelper.hasNotificationPermission(context)
}

private fun isGranted(context: android.content.Context, type: PermissionType): Boolean =
    when (type) {
        PermissionType.UsageAccess   -> PermissionHelper.hasUsageAccess(context)
        PermissionType.Overlay       -> PermissionHelper.hasOverlayPermission(context)
        PermissionType.Notifications -> PermissionHelper.hasNotificationPermission(context)
    }

private fun moveToNextStep(
    stepsSize: Int,
    currentIndex: Int,
    setIndex: (Int) -> Unit,
    setPage: (PermissionPage) -> Unit
) {
    if (currentIndex + 1 < stepsSize) {
        setIndex(currentIndex + 1)
        setPage(PermissionPage.IntroOrSingle)
    } else {
        setIndex(stepsSize) // → LaunchedEffect から onFlowFinished が呼ばれる
    }
}
