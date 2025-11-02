//package com.a307.linkcare.feature.auth.data
//
//import com.a307.linkcare.feature.auth.domain.OnboardingForm
//import retrofit2.http.Body
//import retrofit2.http.POST
//
//interface AuthApi {
//    // 로그인
//    @POST("auth/login")
//    suspend fun login(@Body body: Map<String, String>): TokenResponse
//
//    // 회원가입 (온보딩 데이터 전송)
//    @POST("auth/signup")
//    suspend fun signup(@Body form: OnboardingForm): SignupResponse
//}
//
//// 로그인 후 받을 토큰 응답 모델
//data class TokenResponse(
//    val accessToken: String,
//    val refreshToken: String
//)
//
//// 회원가입 응답 모델
//data class SignupResponse(
//    val message: String? = null,
//    val userId: Long? = null
//)
