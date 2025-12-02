package com.a307.linkcare.feature.mypage.data.model.dto

import com.a307.linkcare.feature.mypage.ui.profileedit.Gender

data class Profile(
    val name: String,
    val gender: Gender,
    val birthDate: String,   // yyyy-MM-dd
    val heightCm: Int,
    val weightKg: Int,
    val petName: String?
)