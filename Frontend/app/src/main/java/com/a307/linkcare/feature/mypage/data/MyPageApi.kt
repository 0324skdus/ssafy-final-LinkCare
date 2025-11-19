package com.a307.linkcare.feature.mypage.data

import com.a307.linkcare.feature.mypage.domain.GroupDetailResponse
import com.a307.linkcare.feature.mypage.domain.MyProfileResponse
import com.a307.linkcare.feature.mypage.domain.UpdateProfileRequest
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