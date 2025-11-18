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

        // 기존 모델이 로드되어 있으면 먼저 언로드
        if (repository.isModelLoaded()) {
            val currentModel = repository.getCurrentModel()
            Log.d(tag, "[USECASE] 기존 모델 발견: ${currentModel?.displayName}")
            Log.d(tag, "[USECASE] 기존 모델 언로드 시작...")

            val unloadResult = repository.unloadModel()
            if (unloadResult.isFailure) {
                Log.e(tag, "[USECASE] ❌ 기존 모델 언로드 실패")
                Log.e(tag, "========================================")
                return unloadResult
            }
            Log.d(tag, "[USECASE] ✅ 기존 모델 언로드 완료")
        } else {
            Log.d(tag, "[USECASE] 로드된 모델 없음")
        }

        Log.d(tag, "[USECASE] 새 모델 로드 시작...")
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
