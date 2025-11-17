package com.a307.linkcare.feature.mypage

import com.a307.linkcare.feature.mypage.MyProfileResponse
import com.a307.linkcare.feature.mypage.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface MyPageApi {

    @GET("/api/users/me")
    suspend fun getMyProfile(): MyProfileResponse

    @PUT("/api/users/profile")
    suspend fun updateProfile(
        @Body req: UpdateProfileRequest
    )
}
