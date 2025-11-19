package com.a307.linkcare.feature.ai.domain.model

/**
 * 건강/운동 데이터 모델
 * AI 프롬프트 생성에 사용
 */
data class HealthData(
    // 필수 필드
    val steps: Int,              // 걸음수
    val duration: Int,            // 운동시간 (분)
    val distance: Double,         // 운동거리 (km)
    val kcal: Int,               // 소모 칼로리
    val heartRate: Int,          // 평균 심박수

    // Optional 필드 (Health 모델용) -> care 부분
    val sleepHours: Double? = null,      // 수면시간
    val waterMl: Int? = null,            // 음수량 (ml)
    val bloodPressure: String? = null,   // 혈압 (예: "120/80")

    // 목표 값 (Wellness 모델용) -> health 부문
    val bestMetric: String? = null,      // 목표 지표명 (예: "운동시간")
    val bestValue: String? = null        // 목표 값 (예: "90분")
)
