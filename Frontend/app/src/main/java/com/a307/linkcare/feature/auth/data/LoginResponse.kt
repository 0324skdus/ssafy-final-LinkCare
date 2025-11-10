package com.a307.linkcare.feature.auth.data

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userPk: Long,
    val email: String,
    val name: String
)