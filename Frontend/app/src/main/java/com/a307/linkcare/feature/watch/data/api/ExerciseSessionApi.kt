package com.a307.linkcare.feature.watch.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 운동 세션 상태를 서버와 동기화하기 위한 API
 */
interface ExerciseSessionApi {

    /**
     * 사용자의 운동 세션 상태 업데이트
     * @param userId 사용자 ID
     * @param request 세션 상태 요청 (START, PAUSE, RESUME, STOP)
     */
    @POST("/api/exercise/session/{userId}")
    suspend fun updateSessionState(
        @Path("userId") userId: Long,
        @Body request: SessionStateRequest
    ): SessionStateResponse

    /**
     * 특정 그룹의 현재 운동 중인 사용자 목록 조회
     * @param groupSeq 그룹 ID
     */
    @GET("/api/exercise/session/group/{groupSeq}")
    suspend fun getGroupExercisingSessions(
        @Path("groupSeq") groupSeq: Long
    ): GroupExerciseSessionsResponse

    /**
     * 모든 그룹의 현재 운동 중인 사용자 목록 조회
     */
    @GET("/api/exercise/session/active")
    suspend fun getAllActiveSessions(): AllActiveSessionsResponse
}

/**
 * 세션 상태 요청 DTO
 */
data class SessionStateRequest(
    val state: String, // "START", "PAUSE", "RESUME", "STOP"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 세션 상태 응답 DTO
 */
data class SessionStateResponse(
    val userId: Long,
    val state: String,
    val timestamp: Long
)

/**
 * 그룹의 운동 중인 사용자 목록 응답 DTO
 */
data class GroupExerciseSessionsResponse(
    val groupSeq: Long,
    val exercisingUserIds: List<Long>
)

/**
 * 모든 활성 세션 응답 DTO
 */
data class AllActiveSessionsResponse(
    val exercisingUserIds: List<Long>
)
