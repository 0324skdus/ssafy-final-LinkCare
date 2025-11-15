package com.a307.linkcare.sdk.health.data.remote

import com.a307.linkcare.sdk.health.data.model.ExerciseData
import com.a307.linkcare.sdk.health.domain.sync.AllHealthData
import com.a307.linkcare.sdk.health.domain.sync.DailyHealthData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 스프링 서버 API 인터페이스
 */
interface HealthApiService {

    /**
     * 하루치 건강 데이터 전송
     * POST /api/health/daily
     */
    @POST("api/health/daily")
    suspend fun uploadDailyHealthData(
        @Body data: DailyHealthData
    ): Response<ApiResponse<Unit>>

    /**
     * 전체 건강 데이터 전송 (최초 동기화)
     * POST /api/health/sync-all
     */
    @POST("api/health/sync-all")
    suspend fun uploadAllHealthData(
        @Body data: AllHealthData
    ): Response<ApiResponse<Unit>>

    /**
     * 사용자별 건강 데이터 전송 (userId 포함)
     * POST /api/health/users/{userId}/daily
     */
    @POST("api/health/users/{userId}/daily")
    suspend fun uploadUserDailyHealthData(
        @Path("userId") userId: Int,
        @Body data: DailyHealthData
    ): Response<ApiResponse<Unit>>

    /**
     * 사용자별 전체 건강 데이터 전송
     * POST /api/health/users/{userId}/sync-all
     */
    @POST("api/health/users/{userId}/sync-all")
    suspend fun uploadUserAllHealthData(
        @Path("userId") userId: Int,
        @Body data: AllHealthData
    ): Response<ApiResponse<Unit>>

    /**
     * 운동 데이터만 전송 (워치 병합용)
     * POST /api/health/users/{userId}/exercise-sync
     */
    @POST("api/health/sync/users/{userId}/exercise-sync")
    suspend fun uploadExerciseOnly(
        @Path("userId") userId: Int,
        @Body exercises: ExerciseData
    ): Response<ApiResponse<Unit>>
}

/**
 * API 응답 래퍼
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)