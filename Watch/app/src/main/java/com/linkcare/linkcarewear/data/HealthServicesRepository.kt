@file:OptIn(ExperimentalCoroutinesApi::class)

package com.linkcare.linkcarewear.data

import android.content.Context
import androidx.health.services.client.data.LocationAvailability
import com.linkcare.linkcarewear.di.bindService
import com.linkcare.linkcarewear.service.ExerciseLogger
import com.linkcare.linkcarewear.service.ExerciseService
import com.linkcare.linkcarewear.service.ExerciseServiceState
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
        sessionId: String,
        function: suspend ExerciseService.(String) -> Unit
    ) = coroutineScope.launch {
        binderConnection.runWhenConnected {
            function(it.getService(), sessionId)
        }
    }

    // startExerciseWithSession 수정
    fun startExerciseWithSession(sessionId: String) =
        serviceCallWithSession(sessionId) { id ->
            try {
                errorState.value = null
                startExercise(id)   // 이게 실제 Service.startExercise(sessionId)
            } catch (e: Exception) {
                errorState.value = e.message
                logger.error("Error starting exercise", e.fillInStackTrace())
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
