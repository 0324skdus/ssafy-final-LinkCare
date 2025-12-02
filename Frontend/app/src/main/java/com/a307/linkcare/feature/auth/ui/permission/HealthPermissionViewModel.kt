package com.a307.linkcare.feature.auth.ui.permission

import android.app.Activity
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.domain.usecase.ArePermissionsGrantedUseCase
import com.a307.linkcare.feature.auth.domain.usecase.RequestPermissionsUseCase
import com.samsung.android.sdk.health.data.error.HealthDataException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthPermissionViewModel @Inject constructor(
    private val client: HealthConnectClient,
    private val requestPermissionsUseCase: RequestPermissionsUseCase,
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    ) : ViewModel() {

    sealed interface State {
        object Idle : State
        object NeedPermission : State
        object Granted : State
        data class Error(val msg: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()




    /** 권한 보유 여부만 판단. 실제 요청은 화면(Compose)에서 실행 */
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                val samsungGranted = arePermissionsGrantedUseCase() // healthDataStore 기반
                if (samsungGranted) {
                    _state.value = State.Granted
                    _permissionsGranted.value = true
                } else {
                    _state.value = State.NeedPermission
                    _permissionsGranted.value = false
                }
            } catch (e: HealthDataException) {
                _state.value = State.Error("권한 확인 실패: ${e.message}")
                _permissionsGranted.value = false
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "권한 확인 실패")
                _permissionsGranted.value = false
            }
        }
    }


    /**
     * 권한 요청
     */
    fun requestPermissions(activity: Activity) {
        viewModelScope.launch {
            try {
                val granted = requestPermissionsUseCase(activity)
                _permissionsGranted.value = granted

                if (!granted) {
                }
            } catch (e: HealthDataException) {
                State.Error("권한 요청 실패: ${e.message}")
            }
        }
    }


}
