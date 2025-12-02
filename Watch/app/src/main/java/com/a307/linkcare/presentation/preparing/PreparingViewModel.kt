package com.a307.linkcare.presentation.preparing

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.data.DataLayerManager
import com.a307.linkcare.data.HealthServicesRepository
import com.a307.linkcare.data.ServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PreparingViewModel
@Inject
constructor(
    private val healthServicesRepository: HealthServicesRepository
) : ViewModel() {
    private var sessionId: Long = 0L

    fun startExercise() {
        // ✅ 세션ID 생성
        sessionId = System.currentTimeMillis()

        viewModelScope.launch {
            healthServicesRepository.prepareExercise()
            healthServicesRepository.startExerciseWithSession(sessionId)
            // 모바일에 운동 시작 상태 전송
            DataLayerManager.sendSessionState("START")
        }
    }

    val uiState: StateFlow<PreparingScreenState> =
        healthServicesRepository.serviceState
            .map {
                val isTrackingInAnotherApp =
                    healthServicesRepository
                        .isTrackingExerciseInAnotherApp()
                val hasExerciseCapabilities = healthServicesRepository.hasExerciseCapability()
                toUiState(
                    serviceState = it,
                    isTrackingInAnotherApp = isTrackingInAnotherApp,
                    hasExerciseCapabilities = hasExerciseCapabilities
                )
            }.stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = toUiState(healthServicesRepository.serviceState.value)
            )

    private fun toUiState(
        serviceState: ServiceState,
        isTrackingInAnotherApp: Boolean = false,
        hasExerciseCapabilities: Boolean = true
    ): PreparingScreenState =
        if (serviceState is ServiceState.Disconnected) {
            PreparingScreenState.Disconnected(serviceState, isTrackingInAnotherApp, permissions)
        } else {
            PreparingScreenState.Preparing(
                serviceState = serviceState as ServiceState.Connected,
                isTrackingInAnotherApp = isTrackingInAnotherApp,
                requiredPermissions = permissions,
                hasExerciseCapabilities = hasExerciseCapabilities
            )
        }

    companion object {
        val permissions = buildList {
            add(Manifest.permission.BODY_SENSORS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
            // Health Connect permissions for Android 14+ (API 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add("android.permission.health.READ_HEART_RATE")
                add("android.permission.health.READ_EXERCISE")
                add("android.permission.health.READ_STEPS")
                add("android.permission.health.READ_DISTANCE")
                add("android.permission.health.READ_TOTAL_CALORIES_BURNED")
                add("android.permission.health.READ_ACTIVE_CALORIES_BURNED")
            }
        }
    }
}
