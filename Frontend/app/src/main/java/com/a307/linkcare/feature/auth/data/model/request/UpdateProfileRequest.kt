package com.a307.linkcare.feature.auth.data.model.request

data class UpdateProfileRequest(
    val birth: String,
    val height: Float?,
    val weight: Float?,
    val gender: String,
    val exerciseStartYear: Int?,
    val petName: String?
)
