package com.a307.linkcare.feature.healthgroup.data.api

import com.a307.linkcare.feature.healthgroup.data.model.request.ActualActivity
import com.a307.linkcare.feature.healthgroup.data.model.response.DailyActivitySummaryResponse
import com.a307.linkcare.feature.healthgroup.data.model.response.HealthGroupResponse
import com.a307.linkcare.feature.healthgroup.data.model.request.UpdateGoalRequest
import com.a307.linkcare.feature.healthgroup.data.model.response.WeeklyGroupGoalResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface HealthGroupApi {

    @Multipart
    @POST("/api/groups/health")
    suspend fun createHealthGroup(
        @Header("Authorization") token: String,

        @Part("groupName") groupName: RequestBody,
        @Part("groupDescription") description: RequestBody,
        @Part("capacity") capacity: RequestBody,
        @Part("minCalorie") minCalorie: RequestBody?,
        @Part("minStep") minStep: RequestBody?,
        @Part("minDistance") minDistance: RequestBody?,
        @Part("minDuration") minDuration: RequestBody?,

        // multipart image (optional)
        @Part image: MultipartBody.Part?
    ): Response<HealthGroupResponse>

    // AI로 목표 생성 (없을 때만 호출)
    @POST("/api/groups/{groupSeq}/goals")
    suspend fun generateGroupGoals(
        @Header("Authorization") token: String,
        @Path("groupSeq") groupSeq: Long,
        @Query("requestDate") requestDate: String  // "yyyy-MM-dd" format
    ): Response<WeeklyGroupGoalResponse>

    // 현재 주차 목표 가져오기
    @GET("/api/groups/{groupSeq}/goals/current")
    suspend fun getCurrentGoals(
        @Header("Authorization") token: String,
        @Path("groupSeq") groupSeq: Long
    ): Response<WeeklyGroupGoalResponse>

    // 목표 수정/저장
    @PUT("/api/groups/{groupSeq}/goals")
    suspend fun updateGoal(
        @Header("Authorization") token: String,
        @Path("groupSeq") groupSeq: Long,
        @Body request: UpdateGoalRequest
    ): Response<WeeklyGroupGoalResponse>

    @GET("/api/health/{userSeq}/actual-activity")
    suspend fun getActualActivity(
        @Header("Authorization") token: String,
        @Path("userSeq") userSeq: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ActualActivity>

    @GET("/api/health/{userSeq}/health/daily-activity/{date}")
    suspend fun getDailyActivity(
        @Path("userSeq") userSeq: Int,
        @Path("date") date: LocalDate
    ): Response<DailyActivitySummaryResponse>
}