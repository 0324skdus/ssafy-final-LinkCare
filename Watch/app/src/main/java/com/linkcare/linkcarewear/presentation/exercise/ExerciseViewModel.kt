package com.linkcare.linkcarewear.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkcare.linkcarewear.data.HealthServicesRepository
import com.linkcare.linkcarewear.data.ServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val healthServicesRepository: HealthServicesRepository
) : ViewModel() {

    /** 운동 세션을 구분하기 위한 고유 ID */
    private var sessionId: String = ""

    /** UI에서 관찰할 상태 */
    val uiState: StateFlow<ExerciseScreenState> =
        healthServicesRepository.serviceState
            .map {
                ExerciseScreenState(
                    hasExerciseCapabilities = healthServicesRepository.hasExerciseCapability(),
                    isTrackingAnotherExercise = healthServicesRepository.isTrackingExerciseInAnotherApp(),
                    serviceState = it,
                    exerciseState = (it as? ServiceState.Connected)?.exerciseServiceState,
                    sessionId = sessionId                     // ✅ 세션 ID 포함
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(3_000),
                healthServicesRepository.serviceState.value.let {
                    ExerciseScreenState(
                        hasExerciseCapabilities = true,
                        isTrackingAnotherExercise = false,
                        serviceState = it,
                        exerciseState = (it as? ServiceState.Connected)?.exerciseServiceState,
                        sessionId = sessionId                 // ✅ 초기 상태에도 포함
                    )
                }
            )

    suspend fun isExerciseInProgress(): Boolean =
        healthServicesRepository.isExerciseInProgress()

    /** 운동 시작 시 sessionId를 생성하고 저장 */
    fun startExercise() {
        sessionId = System.currentTimeMillis().toString()      // ✅ 세션 ID 생성
        viewModelScope.launch {
            healthServicesRepository.startExercise()
        }
    }

    fun pauseExercise() {
        healthServicesRepository.pauseExercise()
    }

    fun endExercise() {
        healthServicesRepository.endExercise()
    }

    fun resumeExercise() {
        healthServicesRepository.resumeExercise()
    }
}
