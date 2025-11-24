package com.a307.linkcare.feature.mypage.ui.state

import com.a307.linkcare.feature.mypage.data.model.dto.Profile

sealed class UiState {
    object Loading : UiState()
    data class Success(val profile: Profile) : UiState()
    data class Error(val message: String) : UiState()
}