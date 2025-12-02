package com.a307.linkcare.feature.auth.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET

data class UserResponse(
    @SerializedName("name")
    val nickname: String,
    val points: Int,
    val mainCharacterImageUrl: String?,
    val mainBackgroundImageUrl: String?,
    val petName: String?
)

interface UserApi {
    @GET("api/users/mypage")
    suspend fun getMyInfo(): Response<UserResponse>
}
