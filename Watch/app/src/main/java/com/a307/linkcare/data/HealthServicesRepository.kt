@file:OptIn(ExperimentalCoroutinesApi::class)

package com.a307.linkcare.data

import android.content.Context
import androidx.health.services.client.data.LocationAvailability
import com.a307.linkcare.di.bindService
import com.a307.linkcare.service.ExerciseLogger
import com.a307.linkcare.service.ExerciseService
import com.a307.linkcare.service.ExerciseServiceState
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ActivityRetainedScoped
class HealthServicesRepository
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    val exerciseClientManager: ExerciseClientManager,
    val logger: ExerciseLogger,
    val coroutineScope: CoroutineScope,
    val lifecycle: ActivityRetainedLifecycle
) {
    private val binderConnection =
        lifecycle.bindService<ExerciseService.LocalBinder, ExerciseService>(applicationContext)

    private val exerciseServiceStateUpdates: Flow<ExerciseServiceState> =
        binderConnection.flowWhenConnected(
            ExerciseService.LocalBinder::exerciseServiceState
        )

    private var errorState: MutableStateFlow<String?> = MutableStateFlow(null)

    val serviceState: StateFlow<ServiceState> =
        exerciseServiceStateUpdates
            .combine(errorState) { exerciseServiceState, errorString ->
                ServiceState.Connected(exerciseServiceState.copy(error = errorString))
            }.stateIn(
                coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = ServiceState.Disconnected
            )

    suspend fun hasExerciseCapability(): Boolean = getExerciseCapabilities() != null

    private suspend fun getExerciseCapabilities() =
        exerciseClientManager.getExerciseCapabilities()

    suspend fun isExerciseInProgress(): Boolean =
        exerciseClientManager.exerciseClient.isExerciseInProgress()

    suspend fun isTrackingExerciseInAnotherApp(): Boolean =
        exerciseClientManager.exerciseClient.isTrackingExerciseInAnotherApp()

    fun prepareExercise() = serviceCall { prepareExercise() }

    private fun serviceCall(function: suspend ExerciseService.() -> Unit) =
        coroutineScope.launch {
            binderConnection.runWhenConnected {
                function(it.getService())
            }
        }

    private fun serviceCallWithSession(
        sessionId: Long,
        function: suspend ExerciseService.(Long) -> Unit
    ) = coroutineScope.launch {
        binderConnection.runWhenConnected {
            function(it.getService(), sessionId)
        }
    }

    // startExerciseWithSession 수정
    fun startExerciseWithSession(sessionId: Long) {
        // 🔒 1. 세션 ID가 비어 있거나 잘못된 경우 자동 실행 방지
        if (sessionId == 0L) {
            logger.log("⚠️ Skipping automatic startExerciseWithSession — invalid sessionId: ${0}")
            return
        }

        serviceCallWithSession(sessionId) { id ->
            try {
                errorState.value = null
                // 🔒 2. 서비스 연결 시 자동 실행 방지 (중복 호출 방어)
                if (exerciseClientManager.exerciseClient.isExerciseInProgress()) {
                    logger.log("⚠️ Exercise already in progress — skipping duplicate startExercise()")
                    return@serviceCallWithSession
                }

                startExercise(id)   // ✅ 실제 서비스 시작
            } catch (e: Exception) {
                errorState.value = e.message
                logger.error("Error starting exercise", e.fillInStackTrace())
            }
        }
    }


    fun pauseExercise() = serviceCall { pauseExercise() }

    fun endExercise() = serviceCall { endExercise() }

    fun resumeExercise() = serviceCall { resumeExercise() }
}

/** Store exercise values in the service state. While the service is connected,
 * the values will persist.**/
sealed class ServiceState {
    data object Disconnected : ServiceState()

    data class Connected(
        val exerciseServiceState: ExerciseServiceState
    ) : ServiceState() {
        val locationAvailabilityState: LocationAvailability =
            exerciseServiceState.locationAvailability
    }
}
