package com.a307.linkcare.feature.auth.domain

data class VerifyCodeRequest(val email: String, val code: String)