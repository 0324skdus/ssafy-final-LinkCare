package com.ssafy.linkcaretest.api

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userPk: Long,
    val email: String,
    val name: String
)