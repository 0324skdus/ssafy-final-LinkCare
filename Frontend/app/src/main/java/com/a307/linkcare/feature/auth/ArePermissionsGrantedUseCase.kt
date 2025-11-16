package com.a307.linkcare.feature.auth

import android.app.Activity
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.data.HealthPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArePermissionsGrantedUseCase @Inject constructor(
    private val client: HealthConnectClient
) : ViewModel() {

    sealed interface State {
        object Idle : State
        object Granted : State
        object Requesting : State
        data class Error(val msg: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun check() {
        viewModelScope.launch {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                val needed = HealthPermissions.permissions - granted

                if (needed.isEmpty()) {
                    _state.value = State.Granted
                } else {
                    _state.value = State.Error("필요한 권한이 부족합니다.")
                }

            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "오류 발생")
            }
        }
    }
}
