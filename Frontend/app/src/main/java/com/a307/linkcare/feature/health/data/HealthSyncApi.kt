package com.a307.linkcare.feature.health.data

import com.a307.linkcare.feature.health.domain.DailyHealthDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface HealthSyncApi {
    @POST("/api/health/users/{userId}/daily")
    suspend fun uploadDailyHealthData(
        @Path("userId") userId: Int,
        @Body data: DailyHealthDataDto
    ): Response<ApiResponse<DailyHealthDataDto>>
}

data class ApiResponse<T>(
    val message: String,
    val status: Int,
    val data: T?
)
