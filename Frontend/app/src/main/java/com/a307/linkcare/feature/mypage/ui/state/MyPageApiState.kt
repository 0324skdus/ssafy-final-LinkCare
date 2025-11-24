package com.a307.linkcare.feature.mypage.ui.state

import com.a307.linkcare.feature.mypage.data.model.dto.MyPageUiState

sealed interface MyPageApiState {
    object Loading : MyPageApiState
    data class Success(val uiState: MyPageUiState) : MyPageApiState
    data class Error(val message: String) : MyPageApiState
}