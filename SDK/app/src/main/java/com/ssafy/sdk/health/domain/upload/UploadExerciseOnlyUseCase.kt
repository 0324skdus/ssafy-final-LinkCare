package com.ssafy.sdk.health.domain.upload

import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.data.repository.HealthRepository
import javax.inject.Inject

/**
 * 운동 데이터만 업로드
 */
class UploadExerciseOnlyUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(
        userId: Int,
        exercises: ExerciseData
    ): Result<Unit> {
        return repository.uploadExerciseOnly(userId, exercises)
    }
}