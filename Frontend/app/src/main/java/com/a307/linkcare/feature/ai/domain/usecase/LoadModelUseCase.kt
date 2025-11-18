package com.a307.linkcare.feature.ai.domain.usecase

import android.util.Log
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
import javax.inject.Inject

/**
 * AI 모델 로드 UseCase
 */
class LoadModelUseCase @Inject constructor(
    private val repository: AiModelRepository
) {
    private val tag = "LoadModelUseCase"

    /**
     * 모델 로드 실행
     * @param modelInfo 로드할 모델 정보
     * @return 성공 시 Result.success, 실패 시 Result.failure
     */
    suspend operator fun invoke(modelInfo: ModelInfo): Result<Unit> {
        Log.d(tag, "========================================")
        Log.d(tag, "[USECASE] invoke() 호출")
        Log.d(tag, "[USECASE] 요청 모델: ${modelInfo.displayName}")

        // 우선순위 큐 시스템이 자동으로 모델 교체 처리
        if (repository.isModelLoaded()) {
            val currentModel = repository.getCurrentModel()
            Log.d(tag, "[USECASE] 현재 로드된 모델: ${currentModel?.displayName}")
        } else {
            Log.d(tag, "[USECASE] 로드된 모델 없음")
        }

        Log.d(tag, "[USECASE] 모델 로드 요청 (우선순위 큐 시스템)...")
        val result = repository.loadModel(modelInfo)

        if (result.isSuccess) {
            Log.d(tag, "[USECASE] ✅ 모델 로드 성공!")
        } else {
            Log.e(tag, "[USECASE] ❌ 모델 로드 실패: ${result.exceptionOrNull()?.message}")
        }
        Log.d(tag, "========================================")

        return result
    }
}
