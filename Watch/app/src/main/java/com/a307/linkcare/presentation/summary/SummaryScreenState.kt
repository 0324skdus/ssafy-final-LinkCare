//화면이 표현할 상태(데이터 구조)
package com.a307.linkcare.presentation.summary

import com.a307.linkcare.data.model.WorkoutSummary
import java.time.Duration

data class SummaryScreenState(
    val sessionId: Long = 0L,
    val averageHeartRate: Double,
    val totalDistance: Double,
    val totalCalories: Double,
    val elapsedTime: Duration
)
fun SummaryScreenState.toWorkoutSummary(): WorkoutSummary {
    return WorkoutSummary(
        sessionId = sessionId,
        avgHeartRate = averageHeartRate.toInt(),
        calories = totalCalories.toFloat(),
        distance = totalDistance.toFloat(),
        durationSec = elapsedTime.seconds
    )
}
