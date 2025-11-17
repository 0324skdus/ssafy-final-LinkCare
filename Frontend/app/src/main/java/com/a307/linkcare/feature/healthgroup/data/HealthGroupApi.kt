package com.a307.linkcare.feature.healthgroup.data

import com.a307.linkcare.feature.healthgroup.domain.HealthGroupResponse
import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


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
}
