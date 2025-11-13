package com.a307.linkcare.feature.auth.domain

data class UpdateProfileRequest(
    val birth: String,
    val height: Float?,
    val weight: Float?,
    val gender: String,
    val exerciseStartYear: Int?
)
