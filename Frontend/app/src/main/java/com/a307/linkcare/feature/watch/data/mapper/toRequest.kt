package com.a307.linkcare.feature.watch.data.mapper

import com.a307.linkcare.feature.watch.model.WorkoutSummary
import com.a307.linkcare.feature.workout.data.dto.WorkoutSummaryRequest

fun WorkoutSummary.toRequest(): WorkoutSummaryRequest {
    return WorkoutSummaryRequest(
        sessionId = sessionId,
        avgHeartRate = avgHeartRate,
        calories = calories,
        distance = distance,
        durationSec = durationSec,
        startTime = startTimestamp,
        endTime = endTimestamp
    )
}
