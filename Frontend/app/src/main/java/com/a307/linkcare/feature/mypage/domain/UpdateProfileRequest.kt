package com.a307.linkcare.feature.mypage.domain

import com.a307.linkcare.feature.mypage.ui.Gender
import com.a307.linkcare.feature.mypage.ui.Profile

data class UpdateProfileRequest(
    val birth: String,
    val height: Int,
    val weight: Int,
    val gender: String,
    val exerciseStartYear: Int? = null // 항상 null 보낼 거면 그대로
)

fun Profile.toUpdateRequest(): UpdateProfileRequest =
    UpdateProfileRequest(
        birth = birthDate,
        height = heightCm,
        weight = weightKg,
        gender = when (gender) {
            Gender.MALE -> "남"
            Gender.FEMALE -> "여"
        },
        exerciseStartYear = null
    )
