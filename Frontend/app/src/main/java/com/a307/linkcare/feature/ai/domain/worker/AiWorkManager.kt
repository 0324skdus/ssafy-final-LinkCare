package com.a307.linkcare.feature.ai.domain.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 워크 매니저
 * - 주기적 AI 코멘트 생성 스케줄링
 */
@Singleton
class AiWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PERIODIC_WORK_NAME = "ai_comment_periodic_work"
        const val REPEAT_INTERVAL_HOURS = 1L
    }

    /**
     * 주기적 AI 코멘트 생성 작업 시작
     */
    fun startPeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // 네트워크 필요
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicAiWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // 이미 있으면 유지
            periodicWorkRequest
        )
    }

    /**
     * 주기적 작업 중지
     */
    fun stopPeriodicWork() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * 즉시 실행 (테스트용)
     */
    fun runNow() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PeriodicAiWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }
}
