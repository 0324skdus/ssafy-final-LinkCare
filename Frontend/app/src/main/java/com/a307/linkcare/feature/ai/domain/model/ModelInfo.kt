package com.a307.linkcare.feature.ai.domain.model

/**
 * AI 모델 정보
 * @property filename 모델 파일명 (예: "health_self.gguf")
 * @property displayName UI 표시명 (예: "건강 상담 (본인 관점)")
 * @property type 모델 타입 (SELF, OTHER, OTHER_SHORT)
 */
data class ModelInfo(
    val filename: String,
    val displayName: String,
    val type: ModelType
)
