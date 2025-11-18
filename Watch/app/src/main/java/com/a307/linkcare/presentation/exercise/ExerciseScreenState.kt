package com.a307.linkcare.presentation.exercise

import com.a307.linkcare.data.ServiceState
import com.a307.linkcare.presentation.summary.SummaryScreenState
import com.a307.linkcare.service.ExerciseServiceState
import java.time.Duration

data class ExerciseScreenState(
    val hasExerciseCapabilities: Boolean,
    val isTrackingAnotherExercise: Boolean,
    val serviceState: ServiceState,
    val exerciseState: ExerciseServiceState?,
    val sessionId: Long = 0L
) {
    fun toSummary(sessionId: Long): SummaryScreenState {
        val exerciseMetrics = exerciseState?.exerciseMetrics
        val averageHeartRate = exerciseMetrics?.heartRateAverage ?: Double.NaN
        val totalDistance = exerciseMetrics?.distance ?: 0.0
        val totalCalories = exerciseMetrics?.calories ?: Double.NaN
        val duration = exerciseState?.activeDurationCheckpoint?.activeDuration ?: Duration.ZERO
        return SummaryScreenState(sessionId,averageHeartRate, totalDistance, totalCalories, duration)
    }

    val isEnding: Boolean
        get() = exerciseState?.exerciseState?.isEnding ?: false

    val isEnded: Boolean
        get() = exerciseState?.exerciseState?.isEnded ?: false

    val isPaused: Boolean
        get() = exerciseState?.exerciseState?.isPaused ?: false

    val error: String?
        get() =
            when (serviceState) {
                is ServiceState.Connected -> serviceState.exerciseServiceState.error
                else -> null
            }
}
