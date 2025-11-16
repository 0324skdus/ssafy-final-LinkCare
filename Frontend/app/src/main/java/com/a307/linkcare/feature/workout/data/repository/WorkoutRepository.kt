package com.a307.linkcare.feature.workout.data.repository

import com.a307.linkcare.feature.workout.data.dto.WorkoutSummaryRequest
import com.a307.linkcare.feature.workout.data.remote.WorkoutApi
import javax.inject.Inject

class WorkoutRepository @Inject constructor(
    private val api: WorkoutApi
) {
    suspend fun uploadSummary(request: WorkoutSummaryRequest): Result<Unit> {
        return try {
            api.uploadSummary(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
