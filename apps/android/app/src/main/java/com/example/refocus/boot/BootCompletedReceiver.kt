package com.example.refocus.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.refocus.feature.overlay.OverlayService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("BootCompletedReceiver", "onReceive action=${intent?.action}")
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // ここでは細かい条件判定は行わず、
        // 一旦サービスを起動して OverlayService 側にチェックさせる。
        Log.d("BootCompletedReceiver", "BOOT_COMPLETED → start OverlayService")
        val serviceIntent = Intent(context, OverlayService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
