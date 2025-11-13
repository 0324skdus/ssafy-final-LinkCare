package com.ssafy.sdk.health.domain.upload

import com.ssafy.sdk.health.data.repository.HealthRepository
import com.ssafy.sdk.health.domain.sync.AllHealthData
import com.ssafy.sdk.health.domain.sync.DailyHealthData
import javax.inject.Inject

/**
 * 하루치 데이터 업로드 UseCase
 */
class UploadDailyHealthDataUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(data: DailyHealthData): Result<Unit> {
        return repository.uploadDailyHealthData(data)
    }

    suspend operator fun invoke(userId: Int, data: DailyHealthData): Result<Unit> {
        return repository.uploadUserDailyHealthData(userId, data)
    }
}