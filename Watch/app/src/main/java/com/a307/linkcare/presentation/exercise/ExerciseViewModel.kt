package com.a307.linkcare.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.data.DataLayerManager
import com.a307.linkcare.data.HealthServicesRepository
import com.a307.linkcare.data.ServiceState
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

    /** 운동 세션 고유 ID */
    private var sessionId: Long = 0L

    /** UI 상태 */
    val uiState: StateFlow<ExerciseScreenState> =
        healthServicesRepository.serviceState
            .map {
                ExerciseScreenState(
                    hasExerciseCapabilities = true, // suspend 함수 대신 기본값으로 처리
                    isTrackingAnotherExercise = false,
                    serviceState = it,
                    exerciseState = (it as? ServiceState.Connected)?.exerciseServiceState,
                    sessionId = sessionId
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(3_000),
                ExerciseScreenState(
                    hasExerciseCapabilities = true,
                    isTrackingAnotherExercise = false,
                    serviceState = healthServicesRepository.serviceState.value,
                    exerciseState = (healthServicesRepository.serviceState.value as? ServiceState.Connected)?.exerciseServiceState,
                    sessionId = sessionId
                )
            )

    suspend fun isExerciseInProgress(): Boolean =
        healthServicesRepository.isExerciseInProgress()

    /** ✅ 운동 시작 */
    fun startExercise() {
        sessionId = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                healthServicesRepository.startExerciseWithSession(sessionId)
                // Repository에서 실제 세션이 시작된 후 모바일에 상태 전송
                DataLayerManager.sendSessionState("START")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** ✅ 일시정지 */
    fun pauseExercise() {
        viewModelScope.launch {
            try {
                healthServicesRepository.pauseExercise()
                DataLayerManager.sendSessionState("PAUSE")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** ✅ 재개 */
    fun resumeExercise() {
        viewModelScope.launch {
            try {
                healthServicesRepository.resumeExercise()
                DataLayerManager.sendSessionState("RESUME")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** ✅ 종료 */
    fun endExercise() {
        viewModelScope.launch {
            try {
                healthServicesRepository.endExercise()
                DataLayerManager.sendSessionState("STOP")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
