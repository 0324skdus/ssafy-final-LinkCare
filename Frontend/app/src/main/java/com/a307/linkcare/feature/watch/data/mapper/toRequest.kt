package com.a307.linkcare.feature.watch.data.mapper

import com.a307.linkcare.feature.watch.domain.model.WorkoutSummary
import com.a307.linkcare.feature.workout.domain.dto.WorkoutSummaryRequest

fun WorkoutSummary.toRequest(): WorkoutSummaryRequest {
    return WorkoutSummaryRequest(
        sessionId = sessionId,
        avgHeartRate = avgHeartRate,
        calories = calories,
        distance = distance,
        durationSec = durationSec,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp
    )
}
