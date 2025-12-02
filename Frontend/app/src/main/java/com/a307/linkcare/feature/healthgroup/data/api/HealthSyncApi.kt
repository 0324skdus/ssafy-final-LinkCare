package com.a307.linkcare.feature.healthgroup.data.api



import com.a307.linkcare.feature.healthgroup.data.model.response.AllHealthDataDto
import com.a307.linkcare.feature.healthgroup.data.model.response.ApiResponse
import com.a307.linkcare.feature.healthgroup.data.model.response.DailyHealthDataDto
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

    @POST("/api/health/users/{userId}/sync-all")
    suspend fun uploadUserAllHealthData(
        @Path("userId") userId: Int,
        @Body data: AllHealthDataDto
    ): Response<ApiResponse<AllHealthDataDto>>
}
