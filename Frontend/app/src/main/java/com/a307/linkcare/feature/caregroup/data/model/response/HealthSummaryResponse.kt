package com.a307.linkcare.feature.caregroup.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class HealthSummaryResponse(
    val summary: String,
    val status: HealthStatus,
    val updatedAt: String? = null
)

@Serializable
enum class HealthStatus {
    ANALYZING,
    PERFECT,
    GOOD,
    CAUTION,
    UNKNOWN;

    fun toDisplayText(): String {
        return when (this) {
            ANALYZING -> "분석 중"
            PERFECT -> "완벽"
            GOOD -> "양호"
            CAUTION -> "주의"
            UNKNOWN -> "알 수 없음"
        }
    }
}