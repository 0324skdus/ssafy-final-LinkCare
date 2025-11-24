package com.a307.linkcare.feature.mypage.data.model.dto

import com.a307.linkcare.feature.caregroup.ui.detail.HealthToday

data class MyPageUiState(
    val nickname: String,
    val coinLabel: String,
    val groupCountLabel: String,
    val avatarUrl: String?,
    val avatarBgUrl: String? = null,
    val healthToday: HealthToday? = null
)