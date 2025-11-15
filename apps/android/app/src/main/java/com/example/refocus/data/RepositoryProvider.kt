package com.example.refocus.data

import android.app.Application
import com.example.refocus.data.datastore.TargetsDataStore
import com.example.refocus.data.repository.TargetsRepository

/**
 * Application から Repository を組み立てるヘルパー。
 * 将来 SessionRepository / SettingsRepository もここに集約する。
 */
class RepositoryProvider(
    private val application: Application
) {

    val targetsRepository: TargetsRepository by lazy {
        val dataStore = TargetsDataStore(application)
        TargetsRepository(dataStore)
    }

    // M3 で追加予定:
    // val sessionRepository: SessionRepository by lazy { ... }
    // val settingsRepository: SettingsRepository by lazy { ... }
}
