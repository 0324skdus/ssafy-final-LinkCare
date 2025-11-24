package com.a307.linkcare.feature.caregroup.ui.create

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.caregroup.domain.repository.CareGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class CareGroupCreateViewModel @Inject constructor(
    private val repo: CareGroupRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val msg: String) : UiState()
    }

    private val _state = mutableStateOf<UiState>(UiState.Idle)
    val state: State<UiState> = _state

    fun createGroup(
        name: String,
        description: String,
        capacity: Int,
        isSleepAllowed: Boolean,
        isWaterIntakeAllowed: Boolean,
        isBloodPressureAllowed: Boolean,
        isBloodSugarAllowed: Boolean,
        imagePart: MultipartBody.Part?
    ) = viewModelScope.launch {
        _state.value = UiState.Loading

        val result = repo.createCareGroup(
            name, description, capacity,
            imagePart,
            isSleepAllowed,
            isWaterIntakeAllowed,
            isBloodPressureAllowed,
            isBloodSugarAllowed
        )

        _state.value = result.fold(
            onSuccess = { UiState.Success },
            onFailure = { UiState.Error(it.message ?: "Unknown error") }
        )
    }
}