package com.a307.linkcare.feature.ai.domain.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.a307.linkcare.feature.ai.domain.service.AiCommentService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 주기적 AI 코멘트 생성 Worker
 *
 * - 1시간마다 실행
 * - ① health-self: 케어 그룹용 건강 코멘트 생성
 * - ② wellness-self: 헬스 그룹용 운동 코멘트 생성
 */
@HiltWorker
class PeriodicAiWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiCommentService: AiCommentService
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "PeriodicAiWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(tag, "========================================")
            Log.d(tag, "[PERIODIC_WORK] AI 코멘트 생성 작업 시작")
            Log.d(tag, "[PERIODIC_WORK] 실행 횟수: $runAttemptCount")

            // ① 케어 그룹 건강 코멘트 생성 및 저장
            val careResult = aiCommentService.generateAndSaveCareGroupComments()
            if (careResult.isSuccess) {
                val careComments = careResult.getOrNull() ?: emptyList()
                Log.d(tag, "[PERIODIC_WORK] ✅ 케어 그룹: ${careComments.size}개 생성")
                careComments.forEach {
                    Log.d(tag, "[PERIODIC_WORK]   - $it")
                }
            } else {
                Log.e(tag, "[PERIODIC_WORK] ❌ 케어 그룹 실패: ${careResult.exceptionOrNull()?.message}")
            }

            // ② 헬스 그룹 운동 코멘트 생성 및 저장
            val healthResult = aiCommentService.generateAndSaveHealthGroupComments()
            if (healthResult.isSuccess) {
                val healthComments = healthResult.getOrNull() ?: emptyList()
                Log.d(tag, "[PERIODIC_WORK] ✅ 헬스 그룹: ${healthComments.size}개 생성")
                healthComments.forEach {
                    Log.d(tag, "[PERIODIC_WORK]   - $it")
                }
            } else {
                Log.e(tag, "[PERIODIC_WORK] ❌ 헬스 그룹 실패: ${healthResult.exceptionOrNull()?.message}")
            }

            Log.d(tag, "[PERIODIC_WORK] ✅ 작업 완료")
            Log.d(tag, "========================================")

            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "[PERIODIC_WORK] ❌ 작업 실패", e)
            Log.e(tag, "========================================")

            // 재시도 가능한 실패
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
