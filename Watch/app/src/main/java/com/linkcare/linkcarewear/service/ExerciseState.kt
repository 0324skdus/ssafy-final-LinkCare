package com.linkcare.linkcarewear.service

import android.util.Log
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate.ActiveDurationCheckpoint
import androidx.health.services.client.data.LocationAvailability

data class ExerciseMetrics(
    val heartRate: Double? = null,
    val distance: Double? = null,
    val calories: Double? = null,
    val heartRateAverage: Double? = null
) {
    fun update(
        latestMetrics: DataPointContainer,
        sessionId: Long,
        checkpoint: ActiveDurationCheckpoint?,
        isEnded: Boolean = false,
        startTimeMillis: Long? = null
    ): ExerciseMetrics {
        val newHeartRate =
            latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: heartRate
        val newDistance =
            latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total ?: distance
        val newCalories =
            latestMetrics.getData(DataType.CALORIES_TOTAL)?.total ?: calories
        val newAvgHeartRate =
            latestMetrics.getData(DataType.HEART_RATE_BPM_STATS)?.average ?: heartRateAverage

        // ✅ duration 계산 (fallback 포함)
        val durationSec =
            checkpoint?.activeDuration?.seconds?.toInt()
                ?: startTimeMillis?.let { ((System.currentTimeMillis() - it) / 1000).toInt() }
                ?: 0

        Log.d(
            "MetricsUpdate",
            "update() called → HR=${newHeartRate?.toInt() ?: 0}, Cal=${newCalories?.toInt() ?: 0}, Dur=$durationSec, session=$sessionId"
        )

        // 💡 UI 갱신용 데이터만 반환 (데이터 전송은 ExerciseService에서 처리)
        return copy(
            heartRate = newHeartRate,
            distance = newDistance,
            calories = newCalories,
            heartRateAverage = newAvgHeartRate
        )
    }
}

// 운동 서비스 상태
data class ExerciseServiceState(
    val exerciseState: ExerciseState? = null,
    val exerciseMetrics: ExerciseMetrics = ExerciseMetrics(),
    val exerciseLaps: Int = 0,
    val activeDurationCheckpoint: ActiveDurationCheckpoint? = null,
    val locationAvailability: LocationAvailability = LocationAvailability.UNKNOWN,
    val error: String? = null,
    val exerciseGoal: Set<ExerciseGoal<out Number>> = emptySet()
)
