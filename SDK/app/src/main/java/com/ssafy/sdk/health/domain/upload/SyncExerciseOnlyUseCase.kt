package com.ssafy.sdk.health.domain.upload

import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.domain.sync.exercise.ExerciseReader
import javax.inject.Inject

/**
 * 운동 데이터만 동기화
 */
class SyncExerciseOnlyUseCase @Inject constructor(
    private val exerciseReader: ExerciseReader
) {
    suspend operator fun invoke(): ExerciseData {
        // 오늘 날짜의 운동 데이터만 수집
        return exerciseReader.readToday()
    }
}