package com.a307.linkcare.feature.mypage.data.model.response

data class MyProfileResponse(
    val name: String,
    val birth: String,
    val height: Double,
    val weight: Double,
    val gender: String,
    val exerciseStartYear: Int? = null,
    val petName: String?
)