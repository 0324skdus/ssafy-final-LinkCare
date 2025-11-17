//package com.a307.linkcare.feature.ai.domain.usecase
//
//import com.a307.linkcare.feature.ai.domain.model.ModelInfo
//import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
//import javax.inject.Inject
//
///**
// * AI 모델 로드 UseCase
// */
//class LoadModelUseCase @Inject constructor(
//    private val repository: AiModelRepository
//) {
//    /**
//     * 모델 로드 실행
//     * @param modelInfo 로드할 모델 정보
//     * @return 성공 시 Result.success, 실패 시 Result.failure
//     */
//    suspend operator fun invoke(modelInfo: ModelInfo): Result<Unit> {
//        // 기존 모델이 로드되어 있으면 먼저 언로드
//        if (repository.isModelLoaded()) {
//            repository.unloadModel()
//        }
//
//        return repository.loadModel(modelInfo)
//    }
//}
