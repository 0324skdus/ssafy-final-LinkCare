package com.a307.linkcare.feature.auth.data.api

import com.a307.linkcare.feature.auth.data.model.request.UpdateFcmTokenRequest
import com.a307.linkcare.feature.auth.data.model.request.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query


interface ProfileApi {
    @PUT("/api/users/profile")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): Response<Void>

    @POST("/api/users/initial-setup")
    suspend fun selectInitial(
        @Query("characterId") characterId: Long,
        @Query("petName") petName: String
    ): Response<Void>

    @PUT("/api/users/fcm-token")
    suspend fun updateFcmToken(
        @Body body: UpdateFcmTokenRequest
    ): Response<Void>
}