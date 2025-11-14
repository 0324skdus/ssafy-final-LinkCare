package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.caregroup.domain.CareGroupResponse
import retrofit2.Response
import okhttp3.MultipartBody
import retrofit2.http.*


interface CareGroupApi {

    @Multipart
    @POST("/api/groups/care")
    suspend fun createCareGroup(
        @Header("Authorization") token: String,

        @Query("groupName") groupName: String,
        @Query("groupDescription") description: String,
        @Query("capacity") capacity: Int,
        @Query("isSleepAllowed") isSleepAllowed: Boolean?,
        @Query("isWaterIntakeAllowed") isWaterIntakeAllowed: Boolean?,
        @Query("isBloodPressureAllowed") isBloodPressureAllowed: Boolean?,
        @Query("isBloodSugarAllowed") isBloodSugarAllowed: Boolean?,

        // multipart
        @Part image: MultipartBody.Part?
    ): Response<CareGroupResponse>
}
