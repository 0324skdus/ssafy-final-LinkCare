package com.a307.linkcare.feature.auth.data.api

import com.a307.linkcare.feature.auth.data.model.request.GoogleLoginRequest
import com.a307.linkcare.feature.auth.data.model.request.KakaoLoginRequest
import com.a307.linkcare.feature.auth.data.model.request.LoginRequest
import com.a307.linkcare.feature.auth.data.model.response.LoginResponse
import com.a307.linkcare.feature.auth.data.model.request.SendVerificationCodeRequest
import com.a307.linkcare.feature.auth.data.model.request.SignupRequest
import com.a307.linkcare.feature.auth.data.model.request.UpdateFcmTokenRequest
import com.a307.linkcare.feature.auth.data.model.request.VerifyCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface AuthApi {

    @POST("/api/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<LoginResponse>

    // 카카오 로그인
    @POST("/api/auth/kakao")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginRequest
    ): Response<LoginResponse>

    // FCM 토큰 업데이트
    @PUT("/api/users/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: UpdateFcmTokenRequest
    ): Response<Void>

    // 이메일 중복 확인 (true = 중복, false = 사용 가능)
    @GET("api/auth/check-email")
    suspend fun checkEmail(
        @Query("email") email: String
    ): Response<Boolean>

    // 인증 코드 발송
    @POST("api/auth/send-verification-code")
    suspend fun sendVerificationCode(
        @Body body: SendVerificationCodeRequest
    ): Response<String>

    // 인증 코드 검증
    @POST("api/auth/verify-code")
    suspend fun verifyCode(
        @Body body: VerifyCodeRequest
    ): Response<String>

    // 회원가입
    @POST("api/auth/signup")
    suspend fun signup(
        @Body body: SignupRequest
    ): Response<String>

    // 로그인 
    @Headers("Content-Type: application/json")
    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<LoginResponse>

    // 토큰 재발급
    @POST("api/auth/refresh")
    suspend fun refresh(
        @Header("Authorization") refreshTokenHeader: String
    ): Response<LoginResponse>

    // 로그아웃
    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") accessToken: String
    ): Response<String>
}