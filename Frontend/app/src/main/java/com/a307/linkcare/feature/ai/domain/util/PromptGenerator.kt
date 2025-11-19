package com.a307.linkcare.feature.ai.domain.util

import java.time.LocalDate
import java.time.LocalTime

/**
 * AI 모델용 프롬프트 생성기
 * 사용자 제공 템플릿 기반
 */
object PromptGenerator {

    /**
     * Wellness SELF: 본인 운동 상태 한줄 요약 -> 헬스 부문
     */
    fun generateWellnessSelfPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        bestMetric: String,
        bestValue: String
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // bestMetric에 따른 단위 결정
        val unit = when {
            bestMetric.contains("걸음") -> "보"
            bestMetric.contains("칼로리") -> "kcal"
            bestMetric.contains("시간") -> "분"
            bestMetric.contains("거리") -> "km"
            else -> ""
        }

        return "오늘 2025-11-12 $timeSegment 기준으로\n" +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km,\n" +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm입니다.\n" +
               "오늘 기준으로 ${bestMetric} ${bestValue}${unit}를 달성하면 이상적입니다.\n" +
               "이 데이터를 바탕으로 나의 운동 상태를 한줄로 요약해주세요."
    }

    /**
     * Health SELF: 본인 건강 상태 한줄 요약 -> 케어 부문
     */
    fun generateHealthSelfPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        sleepHours: Double? = null,
        waterMl: Int? = null,
        bloodPressure: String? = null
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // Optional 필드 처리 - 값이 있을 때만 추가
        val optSleep = sleepHours?.let { ", 수면시간 ${it}시간" } ?: ""
        val optWater = waterMl?.let { ", 음수량 ${it}ml" } ?: ""
        val optBp = bloodPressure?.let { ", 혈압 ${it}mmHg" } ?: ""

        return "오늘 2025-11-12 $timeSegment 기준으로\n" +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km,\n" +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm${optSleep}${optWater}${optBp}.\n" +
               "이 데이터를 바탕으로 오늘의 건강 상태를 한줄로 설명해주세요."
    }

    /**
     * Wellness OTHER: 타인 운동 격려 (3문장) -> 헬스 부문
     */
    fun generateWellnessOtherPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        bestMetric: String,
        bestValue: String
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // bestMetric에 따른 단위 결정
        val unit = when {
            bestMetric.contains("걸음") -> "보"
            bestMetric.contains("칼로리") -> "kcal"
            bestMetric.contains("시간")  -> "분"
            bestMetric.contains("거리") -> "km"
            else -> ""
        }

        return "상대의 2025-11-12 $timeSegment 운동 데이터:\n" +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km,\n" +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm입니다.\n" +
               "오늘 기준으로 ${bestMetric} ${bestValue}${unit}를 달성하면 이상적입니다.\n" +
               "이 데이터를 바탕으로 격려, 권유, 응원 또는 칭찬 문장 3개를 작성해주세요."
    }

    /**
     * Health OTHER: 타인 건강 격려 (3문장) -> 케어 부문
     */
    fun generateHealthOtherPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        sleepHours: Double? = null,
        waterMl: Int? = null,
        bloodPressure: String? = null
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // Optional 필드 처리 - 값이 있을 때만 추가
        val optSleep = sleepHours?.let { ", 수면시간 ${it}시간" } ?: ""
        val optWater = waterMl?.let { ", 음수량 ${it}ml" } ?: ""
        val optBp = bloodPressure?.let { ", 혈압 ${it}mmHg" } ?: ""

        return "상대의 2025-11-12 $timeSegment 건강 데이터:\n" +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km,\n" +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm${optSleep}${optWater}${optBp}.\n" +
               "이 데이터를 바탕으로 격려, 응원, 칭찬, 위로 또는 권유 문장 3개를 작성해주세요."
    }

    /**
     * Wellness OTHER_SHORT: 타인 운동 짧은 응원 -> 헬스 부문
     */
    fun generateWellnessShortPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        bestMetric: String,
        bestValue: String
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // bestMetric에 따른 단위 결정
        val unit = when {
            bestMetric.contains("걸음") -> "보"
            bestMetric.contains("칼로리") -> "kcal"
            bestMetric.contains("시간") || bestMetric.contains("분") -> "분"
            bestMetric.contains("거리") -> "km"
            else -> ""
        }

        return "상대의 2025-11-12 $timeSegment 운동 데이터: " +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km, " +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm입니다. " +
               "오늘 기준으로 $bestMetric ${bestValue}${unit}를 달성하면 이상적입니다. " +
               "이 데이터를 바탕으로 20자 내외의 짧은 캐주얼 응원 문장을 작성해주세요."
    }

    /**
     * Health OTHER_SHORT: 타인 건강 짧은 위로 -> 케어 부문
     */
    fun generateHealthShortPrompt(
        steps: Int,
        duration: Int,
        distance: Double,
        kcal: Int,
        heartRate: Int,
        sleepHours: Double? = null,
        waterMl: Int? = null,
        bloodPressure: String? = null
    ): String {
        val date = LocalDate.now()
        val timeSegment = getTimeSegment()

        // Optional 필드 처리 - 값이 있을 때만 추가
        val optSleep = sleepHours?.let { ", 수면시간 ${it}시간" } ?: ""
        val optWater = waterMl?.let { ", 음수량 ${it}ml" } ?: ""
        val optBp = bloodPressure?.let { ", 혈압 ${it}mmHg" } ?: ""

        return "상대의 2025-11-12 ${timeSegment} 건강 데이터: " +
               "걸음수 ${steps}보, 운동시간 ${duration}분, 운동거리 ${distance}km, " +
               "소모 칼로리 ${kcal}kcal, 평균 심박수 ${heartRate}bpm${optSleep}${optWater}${optBp}. " +
               "이 데이터를 바탕으로 20자 내외의 짧은 캐주얼 위로 문장을 작성해주세요."
    }

    private fun getTimeSegment(): String {
        return when (LocalTime.now().hour) {
            in 6..11 -> "오전"
            in 12..17 -> "오후"
            else -> "저녁"
        }
    }
}
