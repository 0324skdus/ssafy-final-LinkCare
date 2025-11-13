package com.a307.linkcare.feature.auth.data

import com.a307.linkcare.feature.auth.domain.UpdateCharacterRequest
import com.a307.linkcare.feature.auth.domain.UpdateProfileRequest
import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
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
}