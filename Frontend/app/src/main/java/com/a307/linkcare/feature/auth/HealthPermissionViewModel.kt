package com.a307.linkcare.feature.auth

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
class HealthPermissionViewModel @Inject constructor(
    private val client: HealthConnectClient
) : ViewModel() {

    sealed interface State {
        object Idle : State
        object NeedPermission : State
        object Granted : State
        data class Error(val msg: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    /** 권한 보유 여부만 판단. 실제 요청은 화면(Compose)에서 실행 */
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                if (!HealthPermissions.permissions.all { granted.contains(it) }) {
                    _state.value = State.NeedPermission
                } else {
                    _state.value = State.Granted
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "권한 확인 실패")
            }
        }
    }
}
