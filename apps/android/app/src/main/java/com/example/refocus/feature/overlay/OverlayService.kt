package com.example.refocus.feature.overlay

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.refocus.R
import com.example.refocus.data.RepositoryProvider
import com.example.refocus.data.repository.TargetsRepository
import com.example.refocus.data.repository.SessionRepository
import com.example.refocus.feature.monitor.ForegroundAppMonitor
import com.example.refocus.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class OverlayService : LifecycleService() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1

        // 前面アプリのポーリング間隔（将来設定に出す）
        private const val DEFAULT_POLLING_INTERVAL_MS = 500L
        // セッションを分割するかどうかの猶予（将来設定に出す）
        private const val DEFAULT_GRACE_PERIOD_MS = 30_000L
    }

    // サービス専用のCoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var targetsRepository: TargetsRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var foregroundAppMonitor: ForegroundAppMonitor
    private lateinit var overlayController: OverlayController

    private var currentForegroundPackage: String? = null
    private var isTimerVisible: Boolean = false

    private var pendingEndPackage: String? = null
    private var pendingEndJob: Job? = null
    private var lastLeaveAtMillis: Long? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        val app = application as Application

        val repositoryProvider = RepositoryProvider(app)
        targetsRepository = repositoryProvider.targetsRepository
        sessionRepository = repositoryProvider.sessionRepository

        foregroundAppMonitor = ForegroundAppMonitor(this)

        // LifecycleService 自身を LifecycleOwner として渡す
        overlayController = OverlayController(
            context = this,
            lifecycleOwner = this,
        )

        startForegroundWithNotification()
        Log.d(TAG, "startForeground done")

        // 権限が揃っていなければ即終了
        if (!canRunOverlay()) {
            Log.w(TAG, "canRunOverlay = false. stopSelf()")
            stopSelf()
            return
        }

        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        overlayController.hideTimer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        // バインドは使わない
        return null
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundWithNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Refocus timer",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Refocus timer overlay service"
        }
        nm.createNotificationChannel(channel)

        // アイコンはとりあえずアプリアイコンを流用
        val notification: Notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Refocus が動作しています")
                .setContentText("対象アプリ利用時に経過時間を可視化します")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun canRunOverlay(): Boolean {
        val hasUsage = PermissionHelper.hasUsageAccess(this)
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        Log.d(TAG, "hasUsage=$hasUsage, hasOverlay=$hasOverlay")
        return hasUsage && hasOverlay
    }

    /**
     * 前面アプリと対象アプリ集合の変化から導かれる「オーバーレイに対する指示」。
     * M3 でこのイベントに応じてセッションの開始/終了も処理できるようにする。
     */
    private enum class OverlayEvent {
        ShowTimer,
        HideTimer,
        NoChange
    }

    /**
     * 前回の状態 (currentForegroundPackage) と新しい状態から、
     * タイマーを表示/非表示にすべきかを決定する。
     */
    private fun reduceForegroundChange(
        newForegroundPackage: String?,
        targets: Set<String>
    ): OverlayEvent {
        val wasTarget = currentForegroundPackage != null &&
                targets.contains(currentForegroundPackage!!)
        val isTargetNow = newForegroundPackage != null &&
                targets.contains(newForegroundPackage)

        // state 更新
        currentForegroundPackage = newForegroundPackage

        return when {
            !wasTarget && isTargetNow -> {
                Log.d(TAG, "OverlayEvent.ShowTimer for $newForegroundPackage")
                OverlayEvent.ShowTimer
            }
            wasTarget && !isTargetNow -> {
                Log.d(TAG, "OverlayEvent.HideTimer (left target app)")
                OverlayEvent.HideTimer
            }
            else -> OverlayEvent.NoChange
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            combine(
                foregroundAppMonitor.foregroundAppFlow(pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS),
                targetsRepository.observeTargets()
            ) { foregroundPackage, targets ->
                foregroundPackage to targets
            }.collectLatest { (foregroundPackage, targets) ->
                Log.d(TAG, "combine: foreground=$foregroundPackage, targets=$targets")
                val previousPackage = currentForegroundPackage
                try {
                    val event = reduceForegroundChange(foregroundPackage, targets)
                    when (event) {
                        OverlayEvent.ShowTimer -> {
                            val base = SystemClock.elapsedRealtime()
                            val nowMillis = System.currentTimeMillis()
                            val pkg = currentForegroundPackage
                            Log.d(TAG, "showTimer base=$base, nowMillis=$nowMillis")
                            if (pkg != null) {
                                if (pendingEndPackage == pkg) {
                                    Log.d(TAG, "Resumed $pkg within grace period, keep session")
                                    cancelGracePeriodIfAny()
                                } else {
                                    serviceScope.launch {
                                        try {
                                            sessionRepository.startSession(
                                                packageName = pkg,
                                                startedAtMillis = nowMillis
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to start session for $pkg", e)
                                        }
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                overlayController.showTimer(initialElapsedMillis = 0L)
                            }
                            isTimerVisible = true
                        }
                        OverlayEvent.HideTimer -> {
                            val nowMillis = System.currentTimeMillis()
                            Log.d(TAG, "hideTimer by event, nowMillis=$nowMillis")
                            withContext(Dispatchers.Main) {
                                overlayController.hideTimer()
                            }
                            isTimerVisible = false
                            if (previousPackage != null) {
                                startGracePeriod(
                                    packageName = previousPackage,
                                    leaveAtMillis = nowMillis
                                )
                            }
                        }
                        OverlayEvent.NoChange -> {
                            // 何もしない（ログも不要なら省略可）
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in startMonitoring loop", e)
                    // ここで落ちるとサービスごと死ぬので握りつぶす
                    withContext(Dispatchers.Main) {
                        overlayController.hideTimer()
                    }
                    isTimerVisible = false
                }
            }
        }
    }

    /**
     * 現在の対象アプリを「t ミリ秒後にセッション終了する」という猶予状態にする。
     * 猶予中に再度 ShowTimer が来た場合はキャンセルされる。
     */
    private fun startGracePeriod(
        packageName: String,
        leaveAtMillis: Long
    ) {
        // 既存の猶予があればキャンセル
        pendingEndJob?.cancel()

        pendingEndPackage = packageName
        lastLeaveAtMillis = leaveAtMillis

        pendingEndJob = serviceScope.launch {
            try {
                delay(DEFAULT_GRACE_PERIOD_MS)

                // 猶予中に戻ってきた場合は pendingEndPackage が null にされているはず
                if (pendingEndPackage != packageName) {
                    Log.d(TAG, "Grace period job: package changed, do nothing")
                    return@launch
                }

                Log.d(TAG, "Grace period expired for $packageName, ending session")
                sessionRepository.endActiveSession(
                    packageName = packageName,
                    endedAtMillis = leaveAtMillis
                )

                // 状態リセット
                pendingEndPackage = null
                lastLeaveAtMillis = null
            } catch (e: CancellationException) {
                Log.d(TAG, "Grace period canceled for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error in grace period job", e)
            }
        }
    }

    /**
     * 猶予状態を解除する（ユーザが猶予時間内に同じアプリへ戻ってきたなど）。
     */
    private fun cancelGracePeriodIfAny() {
        pendingEndJob?.cancel()
        pendingEndJob = null
        pendingEndPackage = null
        lastLeaveAtMillis = null
    }
}
