package com.example.refocus.feature.overlay

import android.annotation.SuppressLint
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
import com.example.refocus.data.datastore.TargetsDataStore
import com.example.refocus.data.repository.TargetsRepository
import com.example.refocus.feature.monitor.ForegroundAppMonitor
import com.example.refocus.permissions.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : LifecycleService() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    // サービス専用のCoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var targetsRepository: TargetsRepository
    private lateinit var foregroundAppMonitor: ForegroundAppMonitor
    private lateinit var overlayController: OverlayController

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // DataStore → Repository を組み立て
        val targetsDataStore = TargetsDataStore(applicationContext)
        targetsRepository = TargetsRepository(targetsDataStore)

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

    private fun startMonitoring() {
        serviceScope.launch {
            combine(
                foregroundAppMonitor.foregroundAppFlow(pollingIntervalMs = 500L),
                targetsRepository.observeTargets()
            ) { foregroundPackage, targets ->
                foregroundPackage to targets
            }.collectLatest { (foregroundPackage, targets) ->
                Log.d(TAG, "combine: foreground=$foregroundPackage, targets=$targets")
                try {
                    if (foregroundPackage != null && targets.contains(foregroundPackage)) {
                        Log.d(TAG, "showTimer for $foregroundPackage")
                        // 対象アプリが前面 → タイマー表示（UIはMainスレッドへ）
                        val base = SystemClock.elapsedRealtime()
                        withContext(Dispatchers.Main) {
                            overlayController.showTimer(initialElapsedMillis = 0L)
                        }
                    } else {
                        Log.d(TAG, "hideTimer: foreground=$foregroundPackage not in targets")
                        // 対象外 → タイマー非表示（UIはMainスレッドへ）
                        withContext(Dispatchers.Main) {
                            overlayController.hideTimer()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in startMonitoring loop", e)
                    // ここで落ちるとサービスごと死ぬので握りつぶす
                    withContext(Dispatchers.Main) {
                        overlayController.hideTimer()
                    }
                }
            }
        }
    }
}
