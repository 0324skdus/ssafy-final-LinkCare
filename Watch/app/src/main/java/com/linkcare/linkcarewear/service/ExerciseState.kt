/*화면이 표현할 상태(데이터 구조)
*/
package com.linkcare.linkcarewear.service

import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate.ActiveDurationCheckpoint
import androidx.health.services.client.data.LocationAvailability
import com.linkcare.linkcarewear.data.DataLayerManager

data class ExerciseMetrics(
    val heartRate: Double? = null,
    val distance: Double? = null,
    val calories: Double? = null,
    val heartRateAverage: Double? = null
) {
    fun update(
        latestMetrics: DataPointContainer,
        sessionId: String,
        checkpoint: ActiveDurationCheckpoint?,
        isEnded: Boolean = false

    ): ExerciseMetrics {
        val newHeartRate =
            latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: heartRate
        val newDistance =
            latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total ?: distance
        val newCalories =
            latestMetrics.getData(DataType.CALORIES_TOTAL)?.total ?: calories
        val newAvgHeartRate =
            latestMetrics.getData(DataType.HEART_RATE_BPM_STATS)?.average ?: heartRateAverage

        //durationSec 계산 (밀리초 → 초)
        val durationSec = checkpoint?.activeDuration?.seconds?.toInt() ?: 0

        if (!isEnded &&sessionId.isNotEmpty() && newHeartRate != null ) {
            DataLayerManager.sendMetrics(
                sessionId = sessionId,
                heartRate = newHeartRate.toInt(),
                calories = newCalories?.toInt() ?: 0,
                durationSec = durationSec
            )
        }

        return copy(
            heartRate = newHeartRate,
            distance = newDistance,
            calories = newCalories,
            heartRateAverage = newAvgHeartRate)
    }
}

    // Capturing most of the values associated with our exercise in a data class
data class ExerciseServiceState(
    val exerciseState: ExerciseState? = null,
    val exerciseMetrics: ExerciseMetrics = ExerciseMetrics(),
    val exerciseLaps: Int = 0,
    val activeDurationCheckpoint: ActiveDurationCheckpoint? = null,
    val locationAvailability: LocationAvailability = LocationAvailability.UNKNOWN,
    val error: String? = null,
    val exerciseGoal: Set<ExerciseGoal<out Number>> = emptySet()
)
