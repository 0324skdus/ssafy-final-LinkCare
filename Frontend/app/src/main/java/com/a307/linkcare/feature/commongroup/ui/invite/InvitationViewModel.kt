package com.a307.linkcare.feature.commongroup.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.data.model.dto.PermissionAgreementDto
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationPreviewResponse
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitationViewModel @Inject constructor(
    private val repo: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Preview(val data: InvitationPreviewResponse) : UiState()
        object JoinSuccess : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadPreview(token: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repo.getInvitationPreview(token)
            _uiState.value = result.fold(
                onSuccess = { UiState.Preview(it) },
                onFailure = { UiState.Error(it.message ?: "초대 정보를 불러올 수 없습니다") }
            )
        }
    }

    fun joinGroup(token: String, permissions: PermissionAgreementDto) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repo.joinGroupByInvitation(token, permissions)
            _uiState.value = result.fold(
                onSuccess = { UiState.JoinSuccess },
                onFailure = { UiState.Error(it.message ?: "그룹 참가에 실패했습니다") }
            )
        }
    }
}