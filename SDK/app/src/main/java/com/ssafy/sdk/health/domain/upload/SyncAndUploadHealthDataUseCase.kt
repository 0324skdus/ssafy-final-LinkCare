package com.ssafy.sdk.health.domain.upload

import com.ssafy.sdk.health.domain.sync.SyncAllHealthDataUseCase
import com.ssafy.sdk.health.domain.sync.SyncDailyHealthDataUseCase
import javax.inject.Inject

/**
 * 동기화 & 업로드를 한 번에 처리하는 UseCase
 */
class SyncAndUploadHealthDataUseCase @Inject constructor(
    private val syncDailyHealthDataUseCase: SyncDailyHealthDataUseCase,
    private val syncAllHealthDataUseCase: SyncAllHealthDataUseCase,
    private val uploadDailyHealthDataUseCase: UploadDailyHealthDataUseCase,
    private val uploadAllHealthDataUseCase: UploadAllHealthDataUseCase,
    // 운동 데이터 전용
    private val syncExerciseOnlyUseCase: SyncExerciseOnlyUseCase,
    private val uploadExerciseOnlyUseCase: UploadExerciseOnlyUseCase
) {
    /**
     * 하루치 데이터 동기화 & 업로드
     */
    suspend fun syncAndUploadDaily(userId: Int? = 0): Result<Unit> {
        return try {
            // 1. 데이터 수집
            val data = syncDailyHealthDataUseCase()

            // 2. 서버 전송
            if (userId != null) {
                uploadDailyHealthDataUseCase(userId, data)
            } else {
                uploadDailyHealthDataUseCase(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 전체 데이터 동기화 & 업로드
     */
    suspend fun syncAndUploadAll(
        userId: Int? = 0,
        onProgress: (String, Int, Int) -> Unit
    ): Result<Unit> {
        return try {
            // 1. 전체 데이터 수집
            val data = syncAllHealthDataUseCase(onProgress)

            // 2. 서버 전송
            if (userId != null) {
                uploadAllHealthDataUseCase(userId, data)
            } else {
                uploadAllHealthDataUseCase(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 운동 데이터만 동기화 & 업로드
     */
    suspend fun syncAndUploadExerciseOnly(userId: Int): Result<Unit> {
        return try {
            // 1. 오늘의 운동 데이터 수집
            val exerciseData = syncExerciseOnlyUseCase()

            // 2. 서버 전송
            uploadExerciseOnlyUseCase(userId, exerciseData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}