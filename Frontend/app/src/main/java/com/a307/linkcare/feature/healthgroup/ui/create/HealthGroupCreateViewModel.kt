package com.a307.linkcare.feature.healthgroup.ui.create

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.healthgroup.domain.repository.HealthGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject
import androidx.compose.runtime.State

@HiltViewModel
class HealthGroupCreateViewModel @Inject constructor(
    private val repo: HealthGroupRepository
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
        minCalorie: Float?,
        minStep: Int?,
        minDistance: Float?,
        minDuration: Int?,
        imagePart: MultipartBody.Part?
    ) = viewModelScope.launch {
        _state.value = UiState.Loading

        val result = repo.createHealthGroup(
            name, description, capacity,
            minCalorie, minStep, minDistance, minDuration,
            imagePart
        )

        _state.value = result.fold(
            onSuccess = { UiState.Success },
            onFailure = { UiState.Error(it.message ?: "Unknown error") }
        )
    }
}
