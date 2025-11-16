package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.caregroup.domain.CareGroupResponse
import com.a307.linkcare.feature.caregroup.domain.DailyHealthDetailResponse
import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*


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
}
