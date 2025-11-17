package com.a307.linkcare.feature.ai.domain.model

/**
 * AI 모델 타입
 * - SELF: 1문장 생성 (본인 관점)
 * - OTHER: 3문장 생성 (타인 관점)
 * - OTHER_SHORT: 10문장 생성 (짧은 응답)
 */
enum class ModelType {
    SELF,
    OTHER,
    OTHER_SHORT
}
