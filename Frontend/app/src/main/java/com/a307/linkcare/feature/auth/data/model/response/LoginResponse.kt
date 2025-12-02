package com.a307.linkcare.feature.auth.data.model.response

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userPk: Long,
    val email: String,
    val name: String,
    val needsProfileCompletion: Boolean
)