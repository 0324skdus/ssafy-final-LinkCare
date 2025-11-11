package com.ssafy.sdk.health.domain.upload

import com.ssafy.sdk.health.data.repository.HealthRepository
import com.ssafy.sdk.health.domain.sync.AllHealthData
import com.ssafy.sdk.health.domain.sync.DailyHealthData
import javax.inject.Inject


/**
 * 전체 데이터 업로드 UseCase
 */
class UploadAllHealthDataUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(data: AllHealthData): Result<Unit> {
        return repository.uploadAllHealthData(data)
    }

    suspend operator fun invoke(userId: Int, data: AllHealthData): Result<Unit> {
        return repository.uploadUserAllHealthData(userId, data)
    }
}