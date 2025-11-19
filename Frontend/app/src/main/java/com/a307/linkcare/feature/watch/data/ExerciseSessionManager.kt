package com.a307.linkcare.feature.watch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseSessionManager @Inject constructor() {

    // 현재 운동 중인 사용자 ID 목록
    private val _exercisingUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val exercisingUserIds: StateFlow<Set<Long>> = _exercisingUserIds.asStateFlow()

    /**
     * 세션 상태 업데이트
     * @param userId 사용자 ID
     * @param state 세션 상태 ("START", "PAUSE", "RESUME", "STOP")
     */
    fun updateSessionState(userId: Long, state: String) {
        when (state.uppercase()) {
            "START", "RESUME" -> {
                _exercisingUserIds.value = _exercisingUserIds.value + userId
            }
            "PAUSE", "STOP" -> {
                _exercisingUserIds.value = _exercisingUserIds.value - userId
            }
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
