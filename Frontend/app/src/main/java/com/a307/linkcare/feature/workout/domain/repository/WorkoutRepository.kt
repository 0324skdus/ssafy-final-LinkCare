package com.a307.linkcare.feature.workout.domain.repository

import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.workout.domain.dto.WorkoutSummaryRequest
import com.a307.linkcare.feature.workout.data.api.WorkoutApi
import javax.inject.Inject

class WorkoutRepository @Inject constructor(
    private val api: WorkoutApi,
    private val tokenStore: TokenStore
) {
    suspend fun uploadSummary(request: WorkoutSummaryRequest): Result<Unit> {
        return try {
            val userSeq = tokenStore.getUserPk()?.toInt()
                ?: return Result.failure(IllegalStateException("userSeq is null"))

            val response = api.uploadSummary(request, userSeq)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
