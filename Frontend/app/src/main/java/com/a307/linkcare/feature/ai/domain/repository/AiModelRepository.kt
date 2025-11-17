package com.a307.linkcare.feature.ai.domain.repository

import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

/**
 * AI 모델 Repository 인터페이스
 * Domain Layer에서 정의하고 Data Layer에서 구현
 */
interface AiModelRepository {

    /**
     * 사용 가능한 모델 목록 조회
     */
    suspend fun getAvailableModels(): List<ModelInfo>

    /**
     * 모델 로드
     * @param modelInfo 로드할 모델 정보
     */
    suspend fun loadModel(modelInfo: ModelInfo): Result<Unit>

    /**
     * 모델 언로드 (메모리 해제)
     */
    suspend fun unloadModel(): Result<Unit>

    /**
     * 메시지 전송 및 응답 수신 (실시간 스트리밍)
     * @param message 사용자 메시지
     * @return 토큰 단위로 생성되는 응답 Flow
     */
    fun sendMessage(message: String): Flow<String>

    /**
     * 현재 로드된 모델 정보
     */
    suspend fun getCurrentModel(): ModelInfo?

    /**
     * 모델 로드 상태 확인
     */
    suspend fun isModelLoaded(): Boolean
}
