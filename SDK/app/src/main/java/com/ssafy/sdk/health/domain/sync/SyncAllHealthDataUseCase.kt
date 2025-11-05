package com.ssafy.sdk.health.domain.sync


import com.samsung.android.sdk.health.data.DeviceManager
import com.ssafy.sdk.health.domain.sync.activitySummary.ActivitySummaryReader
import com.ssafy.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.ssafy.sdk.health.domain.sync.exercise.ExerciseReader
import com.ssafy.sdk.health.domain.sync.heartRate.HeartRateReader
import com.ssafy.sdk.health.domain.sync.sleep.SleepReader
import com.ssafy.sdk.health.domain.sync.step.StepReader
import com.ssafy.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 전체 동기화 UseCase (최초 1회, 느림)
 *
 * 성능 최적화 옵션:
 * - useParallel: 여러 데이터를 병렬로 조회 (기본 true)
 * - yearsPerChunk: 한 번에 조회할 연도 수 (기본 1년, 클수록 빠르지만 메모리 사용 증가)
 */
class SyncAllHealthDataUseCase @Inject constructor(
    private val heartRateReader: HeartRateReader,
    private val sleepReader: SleepReader,
    private val waterIntakeReader: WaterIntakeReader,
    private val bloodPressureReader: BloodPressureReader,
    private val exerciseReader: ExerciseReader,
    private val stepReader: StepReader,
    private val activitySummaryReader: ActivitySummaryReader,
    private val deviceManager: DeviceManager
) {
    /**
     * @param onProgress 진행 상황 콜백 (데이터 타입, 현재 진행, 전체)
     * @param useParallel 병렬 처리 활성화 (기본 true)
     * @param yearsPerChunk 한 번에 조회할 연도 수 (기본 1년)
     */
    suspend operator fun invoke(
        onProgress: (String, Int, Int) -> Unit,
        useParallel: Boolean = true,
        yearsPerChunk: Long = 1L
    ): AllHealthData {
        return if (useParallel) {
            // 병렬 처리: 여러 데이터를 동시에 조회 (속도 향상)
            syncAllParallel(onProgress, yearsPerChunk)
        } else {
            // 순차 처리: 하나씩 조회 (안정적)
            syncAllSequential(onProgress, yearsPerChunk)
        }
    }

    /**
     * 병렬 처리: 모든 데이터를 동시에 조회
     * 장점: 빠름 (약 5배 속도 향상)
     * 단점: 메모리 사용량 증가, API 부하 증가
     */
    private suspend fun syncAllParallel(
        onProgress: (String, Int, Int) -> Unit,
        yearsPerChunk: Long,
    ): AllHealthData = coroutineScope {
        val heartRateDeferred = async {
            heartRateReader.readAll(
                yearsPerChunk = yearsPerChunk,
                onProgress = { done, total ->
                    onProgress("심박수", done, total)
                },
            )
        }

        val sleepDeferred = async {
            sleepReader.readAll(
                yearsPerChunk = yearsPerChunk,
                onProgress = { done, total ->
                    onProgress("수면", done, total)
                },
                true,
            )
        }

        val waterIntakeDeferred = async {
            waterIntakeReader.readAll(
                yearsPerChunk = yearsPerChunk,
                onProgress = { done, total ->
                    onProgress("음수량", done, total)
                },
            )
        }

        val bloodPressureDeferred = async {
            bloodPressureReader.readAll(
                yearsPerChunk = yearsPerChunk,
                onProgress = { done, total ->
                    onProgress("혈압", done, total)
                },
            )
        }

        val exerciseDeferred = async {
            exerciseReader.readAll(
                onProgress = { done, total ->
                    onProgress("운동", done, total)
                }
            )
        }

        val stepDeferred = async {
            stepReader.readAll(
                onProgress = { done, total ->
                    onProgress("걸음수", done, total)
                },
            )
        }

        val activitySummaryDeferred = async {
            activitySummaryReader.readAll(
                onProgress = { done, total ->
                    onProgress("활동 요약", done, total)
                },
            )
        }

        AllHealthData(
            heartRate = heartRateDeferred.await(),
            sleep = sleepDeferred.await(),
            waterIntake = waterIntakeDeferred.await(),
            bloodPressure = bloodPressureDeferred.await(),
            exercise = exerciseDeferred.await(),
            step = stepDeferred.await(),
            activitySummary = activitySummaryDeferred.await(),
        )
    }

    /**
     * 순차 처리: 하나씩 차례로 조회
     * 장점: 안정적, 메모리 효율적
     * 단점: 느림
     */
    private suspend fun syncAllSequential(
        onProgress: (String, Int, Int) -> Unit,
        yearsPerChunk: Long,
    ): AllHealthData {
        val heartRate = heartRateReader.readAll(
            yearsPerChunk = yearsPerChunk,
            onProgress = { done, total ->
                onProgress("심박수", done, total)
            },
            true,
        )

        val sleep = sleepReader.readAll(
            yearsPerChunk = yearsPerChunk,
            onProgress = { done, total ->
                onProgress("수면", done, total)
            },
            true,
        )

        val waterIntake = waterIntakeReader.readAll(
            onProgress = { done, total ->
                onProgress("음수량", done, total)
            },

        )

        val bloodPressure = bloodPressureReader.readAll(
            yearsPerChunk = yearsPerChunk,
            onProgress = { done, total ->
                onProgress("혈압", done, total)
            }
        )

        val exercise = exerciseReader.readAll(
            onProgress = { done, total ->
                onProgress("운동", done, total)
            }
        )

        val step = stepReader.readAll(
            onProgress = { done, total ->
                onProgress("걸음수", done, total)
            }
        )

        val activitySummary = activitySummaryReader.readAll(
            onProgress = { done, total ->
                onProgress("활동 요약", done, total)
            }
        )

        return AllHealthData(
            heartRate = heartRate,
            sleep = sleep,
            waterIntake = waterIntake,
            bloodPressure = bloodPressure,
            exercise = exercise,
            step = step,
            activitySummary = activitySummary,
        )
    }
}