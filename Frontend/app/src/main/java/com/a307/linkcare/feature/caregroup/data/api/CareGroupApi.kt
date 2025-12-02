package com.a307.linkcare.feature.caregroup.data.api

import com.a307.linkcare.feature.caregroup.data.model.response.CareGroupResponse
import com.a307.linkcare.feature.caregroup.data.model.response.DailyHealthDetailResponse
import com.a307.linkcare.feature.caregroup.data.model.response.GroupStepStatisticsResponse
import com.a307.linkcare.feature.caregroup.data.model.response.SleepStatisticsResponse
import com.a307.linkcare.feature.caregroup.data.model.response.WeeklyHeaderResponse
import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import java.time.LocalDate


interface CareGroupApi {

    @Multipart
    @POST("/api/groups/care")
    suspend fun createCareGroup(
        @Header("Authorization") token: String,

        @Part("groupName") groupName: RequestBody,
        @Part("groupDescription") description: RequestBody,
        @Part("capacity") capacity: RequestBody,
        @Part("isSleepAllowed") isSleepAllowed: RequestBody?,
        @Part("isWaterIntakeAllowed") isWaterIntakeAllowed: RequestBody?,
        @Part("isBloodPressureAllowed") isBloodPressureAllowed: RequestBody?,
        @Part("isBloodSugarAllowed") isBloodSugarAllowed: RequestBody?,

        // multipart image (optional)
        @Part image: MultipartBody.Part?
    ): Response<CareGroupResponse>

    @GET("/api/health/{userSeq}/today/detail")
    suspend fun getDailyHealthDetail(
        @Path("userSeq") userSeq: Int
    ): Response<DailyHealthDetailResponse>

    @GET("/api/health/{userSeq}/health/daily/{date}")
    suspend fun getDailyHealthDetailByDate(
        @Path("userSeq") userSeq: Int,
        @Path("date") date: LocalDate
    ): Response<DailyHealthDetailResponse>

    @GET("/api/groups/weekly-header/{groupSeq}")
    suspend fun getWeeklyHeader(
        @Path("groupSeq") groupSeq: Long
    ): WeeklyHeaderResponse

    @POST("/api/groups/weekly-header/{groupSeq}/regenerate")
    suspend fun regenerateWeeklyHeader(
        @Path("groupSeq") groupSeq: Long
    ): WeeklyHeaderResponse

    @GET("/api/groups/{groupSeq}/sleep-statistics")
    suspend fun getWeeklySleepStatistics(
        @Path("groupSeq") groupSeq: Long,
        @Query("startDate") startDate: LocalDate,
        @Query("endDate") endDate: LocalDate
    ): SleepStatisticsResponse

    @GET("/api/groups/{groupSeq}/step-statistics")
    suspend fun getGroupStepStatistics(
        @Path("groupSeq") groupSeq: Long
    ): Response<GroupStepStatisticsResponse>

    @GET("/api/groups/{groupSeq}/step-statistics/period")
    suspend fun getGroupStepStatisticsByPeriod(
        @Path("groupSeq") groupSeq: Long,
        @Query("startDate") startDate: LocalDate,
        @Query("endDate") endDate: LocalDate
    ): Response<GroupStepStatisticsResponse>


}
