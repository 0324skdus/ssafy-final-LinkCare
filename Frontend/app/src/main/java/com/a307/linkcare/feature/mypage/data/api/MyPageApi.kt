package com.a307.linkcare.feature.mypage.data.api

import com.a307.linkcare.feature.mypage.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.mypage.data.model.response.MyProfileResponse
import com.a307.linkcare.feature.mypage.data.model.request.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface MyPageApi {

    @GET("/api/users/me")
    suspend fun getMyProfile(): MyProfileResponse

    @PUT("/api/users/profile")
    suspend fun updateProfile(
        @Body req: UpdateProfileRequest
    )

    @GET("/api/groups/{groupSeq}/details")
    suspend fun getMyGroupCharacters(
        @Path("groupSeq") groupSeq: Long
    ): GroupDetailResponse
}