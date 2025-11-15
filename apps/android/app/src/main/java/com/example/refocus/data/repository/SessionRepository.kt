package com.example.refocus.data.repository

import com.example.refocus.core.model.Session
import kotlinx.coroutines.flow.Flow
import com.example.refocus.data.db.dao.SessionDao
import com.example.refocus.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.map

interface SessionRepository {

    /**
     * 対象アプリの新しいセッションを開始する。
     * 既に active session があればそのまま返す（2重開始防止）。
     */
    suspend fun startSession(packageName: String, startedAtMillis: Long): Session

    /**
     * 対象アプリのアクティブセッションを終了する。
     * アクティブセッションがなければ何もしない。
     */
    suspend fun endActiveSession(packageName: String, endedAtMillis: Long)

    /**
     * 全セッションを新しい順で購読（履歴画面用）。
     */
    fun observeAllSessions(): Flow<List<Session>>
}

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun startSession(
        packageName: String,
        startedAtMillis: Long
    ): Session {
        // すでにアクティブセッションがあるなら、それをそのまま使う
        val existing = sessionDao.findActiveSession(packageName)
        if (existing != null) {
            return existing.toDomain()
        }

        val entity = SessionEntity(
            packageName = packageName,
            startedAtMillis = startedAtMillis,
            endedAtMillis = null
        )
        val newId = sessionDao.insertSession(entity)
        return entity.copy(id = newId).toDomain()
    }

    override suspend fun endActiveSession(
        packageName: String,
        endedAtMillis: Long
    ) {
        val active = sessionDao.findActiveSession(packageName) ?: return
        if (active.endedAtMillis != null) return // 既に終わっていたら何もしない

        val updated = active.copy(endedAtMillis = endedAtMillis)
        sessionDao.updateSession(updated)
    }

    override fun observeAllSessions(): Flow<List<Session>> {
        return sessionDao.observeAllSessions()
            .map { list -> list.map { it.toDomain() } }
    }

    // --- Entity <-> Domain 変換 ---

    private fun SessionEntity.toDomain(): Session = Session(
        id = this.id,
        packageName = this.packageName,
        startedAtMillis = this.startedAtMillis,
        endedAtMillis = this.endedAtMillis
    )
}