//화면이 표현할 상태(데이터 구조)
package com.linkcare.linkcarewear.presentation.summary

import com.linkcare.linkcarewear.data.model.WorkoutSummary
import java.time.Duration

data class SummaryScreenState(
    val sessionId: String = "",
    val averageHeartRate: Double,
    val totalDistance: Double,
    val totalCalories: Double,
    val elapsedTime: Duration
)
fun SummaryScreenState.toWorkoutSummary(): WorkoutSummary {
    return WorkoutSummary(
        sessionId = sessionId,
        avgHeartRate = averageHeartRate.toInt(),
        calories = totalCalories.toInt(),
        distance = totalDistance.toInt(),
        durationSec = elapsedTime.seconds.toInt()
    )
}
