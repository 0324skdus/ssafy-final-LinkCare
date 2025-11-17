package com.a307.linkcare.feature.healthgroup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.data.GroupRepository
import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
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

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

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

    fun delegateLeader(groupSeq: Long, newLeaderUserSeq: Long) {
        viewModelScope.launch {
            val result = repo.delegateLeader(groupSeq, newLeaderUserSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹장 위임 성공")
                    // 그룹 상세 정보 다시 로드
                    loadGroupDetail(groupSeq)
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹장 위임 실패: ${error.message}")
                }
            )
        }
    }

    fun kickMember(groupSeq: Long, targetUserSeq: Long) {
        viewModelScope.launch {
            val result = repo.kickMember(groupSeq, targetUserSeq)
            result.fold(
                onSuccess = {
                    Log.d("HealthGroupEditViewModel", "그룹원 내보내기 성공")
                    // 그룹 상세 정보 다시 로드
                    loadGroupDetail(groupSeq)
                },
                onFailure = { error ->
                    Log.e("HealthGroupEditViewModel", "그룹원 내보내기 실패: ${error.message}")
                }
            )
        }
    }
}
