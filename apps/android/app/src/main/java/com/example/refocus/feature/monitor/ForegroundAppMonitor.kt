package com.example.refocus.feature.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ForegroundAppMonitor(
    context: Context
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun foregroundAppFlow(
        pollingIntervalMs: Long = 500L
    ): Flow<String?> = flow {
        var lastPackage: String? = null
        while (true) {
            val now = System.currentTimeMillis()
            val begin = now - 2_000

            val topApp: String? = try {
                val events = usageStatsManager.queryEvents(begin, now)
                var pkg: String? = null
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        pkg = event.packageName
                    }
                }
                Log.d("ForegroundAppMonitor", "queryEvents topApp=$pkg")
                pkg
            } catch (e: SecurityException) {
                Log.w("ForegroundAppMonitor", "SecurityException in queryEvents", e)
                null
            }

            if (topApp != null && topApp != lastPackage) {
                lastPackage = topApp
                Log.d("ForegroundAppMonitor", "emit foreground=$topApp")
                emit(topApp)
            }

            delay(pollingIntervalMs)
        }
    }
}
