//package com.a307.linkcare.feature.auth.data
//
//import com.a307.linkcare.feature.auth.domain.OnboardingForm
//
//class AuthRepository(
//    private val api: AuthApi
//) {
//    suspend fun login(email: String, password: String) =
//        api.login(mapOf("email" to email, "password" to password))
//
//    suspend fun signup(form: OnboardingForm) =
//        api.signup(form)
//}
