package com.a307.linkcare.feature.mypage.ui.profileedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.mypage.data.model.dto.Profile
import com.a307.linkcare.feature.mypage.domain.repository.MyPageRepository
import com.a307.linkcare.feature.mypage.data.model.request.toUpdateRequest
import com.a307.linkcare.feature.mypage.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageEditViewModel @Inject constructor(
    private val repo: MyPageRepository
) : ViewModel() {

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            runCatching {
                repo.getMyProfile() // suspend fun getMyProfile(): MyProfileResponse
            }.onSuccess { response ->
                uiState = UiState.Success(response.toProfile())
            }.onFailure { e ->
                uiState = UiState.Error(e.message ?: "프로필을 불러오지 못했습니다.")
            }
        }
    }

    fun saveProfile(updated: Profile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                // PUT /api/users/profile 요청용 DTO
                val req = updated.toUpdateRequest()
                repo.updateProfile(req) // suspend fun updateProfile(req: UpdateProfileRequest)
            }.onSuccess {
                onSuccess()
                // 저장 후 다시 조회하고 싶으면:
                // loadMyProfile()
            }.onFailure { e ->
                // 에러 처리 (스낵바, 토스트 등은 UI에서)
            }
        }
    }
}