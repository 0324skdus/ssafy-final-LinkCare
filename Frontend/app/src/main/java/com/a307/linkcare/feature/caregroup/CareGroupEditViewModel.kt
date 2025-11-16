package com.a307.linkcare.feature.caregroup

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
class CareGroupEditViewModel @Inject constructor(
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
        isSleepRequired: Boolean? = null,
        isWaterIntakeRequired: Boolean? = null,
        isBloodPressureRequired: Boolean? = null,
        isBloodSugarRequired: Boolean? = null
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            val result = repo.updateGroup(
                groupSeq = groupSeq,
                groupName = groupName,
                groupDescription = groupDescription,
                imageAction = imageAction,
                imageFile = imageFile,
                isSleepRequired = isSleepRequired,
                isWaterIntakeRequired = isWaterIntakeRequired,
                isBloodPressureRequired = isBloodPressureRequired,
                isBloodSugarRequired = isBloodSugarRequired,
                minCalorie = null,
                minStep = null,
                minDistance = null,
                minDuration = null
            )

            _updateState.value = result.fold(
                onSuccess = { UpdateState.Success },
                onFailure = { UpdateState.Error(it.message ?: "Failed to update group") }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
