package com.a307.linkcare.service

import android.annotation.SuppressLint
import android.app.Service
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate
import com.a307.linkcare.data.ExerciseClientManager
import com.a307.linkcare.data.ExerciseMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ExerciseServiceMonitor
@Inject
constructor(
    val exerciseClientManager: ExerciseClientManager,
    val service: Service
) {
    // TODO behind an interface
    val exerciseService = service as ExerciseService

    val exerciseServiceState =
        MutableStateFlow(
            ExerciseServiceState(
                exerciseState = null,
                exerciseMetrics = ExerciseMetrics()
            )
        )

    // 새 운동 시작 전 상태 리셋
    fun resetState() {
        exerciseServiceState.value = ExerciseServiceState(
            exerciseState = null,
            exerciseMetrics = ExerciseMetrics()
        )
    }

    suspend fun monitor() {
        exerciseClientManager.exerciseUpdateFlow.collect {
            when (it) {
                is ExerciseMessage.ExerciseUpdateMessage ->
                    processExerciseUpdate(it.exerciseUpdate)

                is ExerciseMessage.LapSummaryMessage ->
                    exerciseServiceState.update { oldState ->
                        oldState.copy(
                            exerciseLaps = it.lapSummary.lapCount
                        )
                    }

                is ExerciseMessage.LocationAvailabilityMessage ->
                    exerciseServiceState.update { oldState ->
                        oldState.copy(
                            locationAvailability = it.locationAvailability
                        )
                    }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun processExerciseUpdate(exerciseUpdate: ExerciseUpdate) {
        val state = exerciseUpdate.exerciseStateInfo.state
        val isEnded = state.isEnded

        // ✅ ACTIVE 상태가 되었을 때 시작 시간 기록
        if (state == ExerciseState.ACTIVE && exerciseService.startTimestamp == null) {
            exerciseService.startTimestamp = System.currentTimeMillis()
        }

        val sessionId = exerciseService.sessionId

        exerciseServiceState.update { old ->
            old.copy(
                exerciseState = exerciseUpdate.exerciseStateInfo.state,
                exerciseMetrics = old.exerciseMetrics.update(
                    exerciseUpdate.latestMetrics,
                    sessionId = sessionId,
                    checkpoint = exerciseUpdate.activeDurationCheckpoint,
                    isEnded = isEnded,
                    startTimeMillis = exerciseService.startTimestamp
                ),
                activeDurationCheckpoint = exerciseUpdate.activeDurationCheckpoint
                    ?: old.activeDurationCheckpoint,
                exerciseGoal = exerciseUpdate.latestAchievedGoals
            )
        }
    }
}
