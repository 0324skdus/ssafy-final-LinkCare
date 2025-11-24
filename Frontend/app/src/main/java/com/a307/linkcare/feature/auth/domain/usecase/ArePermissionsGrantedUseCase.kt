package com.a307.linkcare.feature.auth.domain.usecase

import androidx.health.connect.client.HealthConnectClient
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import javax.inject.Inject

//@HiltViewModel
class ArePermissionsGrantedUseCase @Inject constructor(
    private val client: HealthConnectClient,
    private val healthDataStore: HealthDataStore
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Boolean {
        val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)
        return grantedPermissions.containsAll(Permissions.PERMISSIONS)
    }
}

//    sealed interface State {
//        object Idle : State
//        object Granted : State
//        object Requesting : State
//        data class Error(val msg: String) : State
//    }
//
//    @Throws(HealthDataException::class)
//    suspend operator fun invoke(): Boolean {
//        val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)
//        val areAllPermissionsGranted = grantedPermissions.containsAll(Permissions.PERMISSIONS)
//        return areAllPermissionsGranted
//    }
//
//
//    private val _state = MutableStateFlow<State>(State.Idle)
//    val state = _state.asStateFlow()
//
//    fun check() {
//        viewModelScope.launch {
//            try {
//                val granted = client.permissionController.getGrantedPermissions()
//                val needed = HealthPermissions.permissions - granted
//
//                if (needed.isEmpty()) {
//                    _state.value = State.Granted
//                } else {
//                    _state.value = State.Error("필요한 권한이 부족합니다.")
//                }
//
//            } catch (e: Exception) {
//                _state.value = State.Error(e.message ?: "오류 발생")
//            }
//        }
//    }
//}

object Permissions {

    val PERMISSIONS = setOf<Permission> (
        // 일일 걸음수
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),

        // 활동 요약
        Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ),

        // 심박수
        Permission.of(DataTypes.HEART_RATE, AccessType.READ),

        // 음수량
        Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
        Permission.of(DataTypes.WATER_INTAKE_GOAL, AccessType.READ),

        // 수면
        Permission.of(DataTypes.SLEEP, AccessType.READ),
        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ),

        // 운동
        Permission.of(DataTypes.EXERCISE, AccessType.READ),

        // 혈압
        Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ),


        )
}



