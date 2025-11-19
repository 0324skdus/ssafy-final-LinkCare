package com.a307.linkcare.feature.watch.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseSessionManager @Inject constructor(
    private val api: ExerciseSessionApi
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 현재 운동 중인 사용자 ID 목록
    private val _exercisingUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val exercisingUserIds: StateFlow<Set<Long>> = _exercisingUserIds.asStateFlow()

    init {
        // 앱 시작 시 서버에서 활성 세션 동기화
        scope.launch {
            syncFromServer()
            // 5초마다 서버와 동기화
            while (true) {
                delay(5000)
                syncFromServer()
            }
        }
    }

    /**
     * 세션 상태 업데이트 (로컬 + 서버)
     * @param userId 사용자 ID
     * @param state 세션 상태 ("START", "PAUSE", "RESUME", "STOP")
     */
    fun updateSessionState(userId: Long, state: String) {
        // 로컬 상태 먼저 업데이트
        when (state.uppercase()) {
            "START", "RESUME" -> {
                _exercisingUserIds.value = _exercisingUserIds.value + userId
            }
            "PAUSE", "STOP" -> {
                _exercisingUserIds.value = _exercisingUserIds.value - userId
            }
        }

        // 서버와 동기화
        scope.launch {
            try {
                api.updateSessionState(
                    userId = userId,
                    request = SessionStateRequest(state = state.uppercase())
                )
                Log.d("ExerciseSessionManager", "✅ Session state synced to server: userId=$userId, state=$state")
            } catch (e: Exception) {
                Log.e("ExerciseSessionManager", "❌ Failed to sync session state to server", e)
            }
        }
    }

    /**
     * 서버에서 활성 세션 동기화
     */
    private suspend fun syncFromServer() {
        try {
            val response = api.getAllActiveSessions()
            _exercisingUserIds.value = response.exercisingUserIds.toSet()
            Log.d("ExerciseSessionManager", "🔄 Synced from server: ${response.exercisingUserIds.size} active sessions")
        } catch (e: Exception) {
            Log.e("ExerciseSessionManager", "❌ Failed to sync from server", e)
        }
    }

    /**
     * 특정 그룹의 운동 중인 사용자 조회
     */
    suspend fun getGroupExercisingSessions(groupSeq: Long): Set<Long> {
        return try {
            val response = api.getGroupExercisingSessions(groupSeq)
            response.exercisingUserIds.toSet()
        } catch (e: Exception) {
            Log.e("ExerciseSessionManager", "❌ Failed to get group sessions", e)
            emptySet()
        }
    }

    /**
     * 특정 사용자가 운동 중인지 확인
     */
    fun isExercising(userId: Long): Boolean {
        return exercisingUserIds.value.contains(userId)
    }

    /**
     * 모든 세션 초기화
     */
    fun clear() {
        _exercisingUserIds.value = emptySet()
    }
}
