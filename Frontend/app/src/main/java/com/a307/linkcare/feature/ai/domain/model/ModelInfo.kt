package com.a307.linkcare.feature.ai.domain.model

/**
 * AI 모델 정보
 * @property filename 모델 파일명 (예: "health_self.gguf")
 * @property displayName UI 표시명 (예: "건강 상담 (본인 관점)")
 * @property category 모델 카테고리 (HEALTH, WELLNESS)
 * @property perspective 모델 관점 (SELF, OTHER, OTHER_SHORT)
 */
data class ModelInfo(
    val filename: String,
    val displayName: String,
    val category: ModelCategory,
    val perspective: ModelPerspective
) {
    /**
     * 기존 코드 호환성을 위한 type 프로퍼티
     */
    @Deprecated("Use perspective instead", ReplaceWith("perspective"))
    val type: ModelType
        get() = when (perspective) {
            ModelPerspective.SELF -> ModelType.SELF
            ModelPerspective.OTHER -> ModelType.OTHER
            ModelPerspective.OTHER_SHORT -> ModelType.OTHER_SHORT
        }

    companion object {
        /**
         * ModelConfig에서 ModelInfo로 변환
         */
        fun fromConfig(config: ModelConfig): ModelInfo {
            return ModelInfo(
                filename = config.filename,
                displayName = config.displayName,
                category = config.category,
                perspective = config.perspective
            )
        }
    }
}
