package com.a307.linkcare.feature.healthgroup.ui.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import com.a307.linkcare.feature.commongroup.data.model.response.GroupDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HealthGroupEditViewModel @Inject constructor(
    private val repo: GroupRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val groupDetail: GroupDetailResponse) : UiState()
        data class Error(val msg: String) : UiState()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val msg: String) : UpdateState()
    }

    sealed class MemberActionState {
        object Idle : MemberActionState()
        object Loading : MemberActionState()
        data class DelegateSuccess(val msg: String) : MemberActionState()
        data class KickSuccess(val msg: String) : MemberActionState()
        data class LeaveSuccess(val msg: String) : MemberActionState()
        data class DeleteSuccess(val msg: String) : MemberActionState()
        data class Error(val msg: String) : MemberActionState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private val _memberActionState = MutableStateFlow<MemberActionState>(MemberActionState.Idle)
    val memberActionState: StateFlow<MemberActionState> = _memberActionState

    fun loadGroupDetail(groupSeq: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val detail = repo.getGroupDetail(groupSeq)
                _uiState.value = UiState.Success(detail)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load group detail")
            }
        }
    }

    fun updateGroup(
        groupSeq: Long,
        groupName: String,
        groupDescription: String,
        imageAction: String, // "keep", "delete", "update"
        imageFile: File? = null,
        minCalorie: Float? = null,
        minStep: Int? = null,
        minDistance: Float? = null,
        minDuration: Int? = null
    ) {
        viewModelScope.launch {
            Log.d("HealthGroupEditVM", "updateGroup 호출: groupSeq=$groupSeq, minCalorie=$minCalorie, minStep=$minStep, minDistance=$minDistance, minDuration=$minDuration")
            _updateState.value = UpdateState.Loading

            val result = repo.updateGroup(
                groupSeq = groupSeq,
                groupName = groupName,
                groupDescription = groupDescription,
                imageAction = imageAction,
                imageFile = imageFile,
                isSleepRequired = null,
                isWaterIntakeRequired = null,
                isBloodPressureRequired = null,
                isBloodSugarRequired = null,
                minCalorie = minCalorie,
                minStep = minStep,
                minDistance = minDistance,
                minDuration = minDuration
            )

            _updateState.value = result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditVM", "그룹 수정 성공")
                    UpdateState.Success
                },
                onFailure = {
                    Log.e("HealthGroupEditVM", "그룹 수정 실패: ${it.message}", it)
                    UpdateState.Error(it.message ?: "Failed to update group")
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun resetMemberActionState() {
        _memberActionState.value = MemberActionState.Idle
    }

    fun delegateLeader(groupSeq: Long, newLeaderUserSeq: Long) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = repo.delegateLeader(groupSeq, newLeaderUserSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹장 위임 성공")
                    _memberActionState.value = MemberActionState.DelegateSuccess("그룹장을 위임했습니다")
                    // 그룹 상세 정보 다시 로드 (백그라운드에서)
                    refreshGroupDetailSilently(groupSeq)
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹장 위임 실패: ${error.message}")
                    _memberActionState.value = MemberActionState.Error(error.message ?: "그룹장 위임에 실패했습니다")
                }
            )
        }
    }

    fun kickMember(groupSeq: Long, targetUserSeq: Long) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = repo.kickMember(groupSeq, targetUserSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹원 내보내기 성공")
                    _memberActionState.value = MemberActionState.KickSuccess("그룹원을 내보냈습니다")
                    // 그룹 상세 정보 다시 로드 (백그라운드에서)
                    refreshGroupDetailSilently(groupSeq)
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹원 내보내기 실패: ${error.message}")
                    _memberActionState.value = MemberActionState.Error(error.message ?: "그룹원 내보내기에 실패했습니다")
                }
            )
        }
    }

    private suspend fun refreshGroupDetailSilently(groupSeq: Long) {
        try {
            val detail = repo.getGroupDetail(groupSeq)
            // UiState가 Success일 때만 업데이트 (Loading 상태로 전환하지 않음)
            if (_uiState.value is UiState.Success) {
                _uiState.value = UiState.Success(detail)
            }
        } catch (e: Exception) {
            Log.e("HealthGroupEditViewModel", "그룹 상세 정보 갱신 실패: ${e.message}")
        }
    }

    fun leaveGroup(groupSeq: Long) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = repo.leaveGroup(groupSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹 탈퇴 성공")
                    _memberActionState.value = MemberActionState.LeaveSuccess("그룹에서 탈퇴했습니다")
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹 탈퇴 실패: ${error.message}")
                    _memberActionState.value = MemberActionState.Error(error.message ?: "그룹 탈퇴에 실패했습니다")
                }
            )
        }
    }

    fun deleteGroup(groupSeq: Long) {
        viewModelScope.launch {
            _memberActionState.value = MemberActionState.Loading
            val result = repo.deleteGroup(groupSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹 삭제 성공")
                    _memberActionState.value = MemberActionState.DeleteSuccess("그룹을 삭제했습니다")
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹 삭제 실패: ${error.message}")
                    _memberActionState.value = MemberActionState.Error(error.message ?: "그룹 삭제에 실패했습니다")
                }
            )
        }
    }
}
