package com.a307.linkcare.feature.mypage

import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
import com.a307.linkcare.feature.mypage.MyProfileResponse
import com.a307.linkcare.feature.mypage.UpdateProfileRequest
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
    ): com.a307.linkcare.feature.mypage.GroupDetailResponse
}
