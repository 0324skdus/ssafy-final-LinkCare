package com.a307.linkcare.feature.healthgroup.data

import com.a307.linkcare.feature.healthgroup.domain.HealthGroupResponse
import retrofit2.Response
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query


interface HealthGroupApi {

    @Multipart
    @POST("/api/groups/health")
    suspend fun createHealthGroup(
        @Header("Authorization") token: String,

        @Query("groupName") groupName: String,
        @Query("groupDescription") description: String,
        @Query("capacity") capacity: Int,
        @Query("minCalorie") minCalorie: Float?,
        @Query("minStep") minStep: Int?,
        @Query("minDistance") minDistance: Float?,
        @Query("minDuration") minDuration: Int?,

        // multipart
        @Part image: MultipartBody.Part?
    ): Response<HealthGroupResponse>
}
