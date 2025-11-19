package com.a307.linkcare.feature.ai.domain.model

/**
 * AI 모델 카테고리
 */
enum class ModelCategory {
    HEALTH,   // 건강(케어) 그룹
    WELLNESS  // 웰니스(운동) 그룹
}

/**
 * AI 모델 관점
 */
enum class ModelPerspective(val priority: Int) {
    OTHER_SHORT(1),  // 타인 관점 짧은 응답 (콕 찌르기) - 최우선
    OTHER(2),        // 타인 관점 (편지) - 중간
    SELF(3);         // 본인 관점 (내 상태 한 문장) - 최하위

    companion object {
        /**
         * 우선순위 비교: 낮은 숫자가 높은 우선순위
         */
        fun compare(a: ModelPerspective, b: ModelPerspective): Int {
            return a.priority.compareTo(b.priority)
        }
    }
}

/**
 * AI 모델 설정
 */
data class ModelConfig(
    val category: ModelCategory,
    val perspective: ModelPerspective,
    val filename: String,
    val displayName: String
)

/**
 * 모델 레지스트리
 */
object ModelRegistry {
    val models = listOf(
        // Health (케어 그룹)
        ModelConfig(
            ModelCategory.HEALTH,
            ModelPerspective.SELF,
            "health_self.gguf",
            "건강 상담 (본인 관점)"
        ),
        ModelConfig(
            ModelCategory.HEALTH,
            ModelPerspective.OTHER,
            "health_other.gguf",
            "건강 상담 (타인 격려)"
        ),
        ModelConfig(
            ModelCategory.HEALTH,
            ModelPerspective.OTHER_SHORT,
            "health_other_short.gguf",
            "건강 상담 (짧은 격려)"
        ),
        // Wellness (헬스 그룹)
        ModelConfig(
            ModelCategory.WELLNESS,
            ModelPerspective.SELF,
            "wellness_self.gguf",
            "웰니스 상담 (본인 관점)"
        ),
        ModelConfig(
            ModelCategory.WELLNESS,
            ModelPerspective.OTHER,
            "wellness_other.gguf",
            "웰니스 상담 (타인 격려)"
        ),
        ModelConfig(
            ModelCategory.WELLNESS,
            ModelPerspective.OTHER_SHORT,
            "wellness_other_short.gguf",
            "웰니스 상담 (짧은 격려)"
        )
    )

    fun getModel(category: ModelCategory, perspective: ModelPerspective): ModelConfig {
        return models.first { it.category == category && it.perspective == perspective }
    }
}

/**
 * 기존 호환성을 위한 ModelType (Deprecated)
 */
@Deprecated("Use ModelPerspective instead", ReplaceWith("ModelPerspective"))
enum class ModelType {
    SELF,
    OTHER,
    OTHER_SHORT;

    fun toPerspective(): ModelPerspective {
        return when (this) {
            SELF -> ModelPerspective.SELF
            OTHER -> ModelPerspective.OTHER
            OTHER_SHORT -> ModelPerspective.OTHER_SHORT
        }
    }
}
