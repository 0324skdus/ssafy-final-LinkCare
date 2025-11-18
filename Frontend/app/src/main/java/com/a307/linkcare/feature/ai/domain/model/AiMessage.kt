package com.a307.linkcare.feature.ai.domain.model

/**
 * AI 대화 메시지
 * @property content 메시지 내용
 * @property isUser true: 사용자 메시지, false: AI 응답
 * @property timestamp 생성 시각
 */
data class AiMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
