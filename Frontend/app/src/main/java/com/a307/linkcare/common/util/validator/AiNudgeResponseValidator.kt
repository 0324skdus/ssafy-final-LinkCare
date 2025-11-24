package com.a307.linkcare.common.util.validator

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * AI 콕 찌르기 메시지 (짧은 격려 ~20자) 검증 및 더미 데이터 제공
 * health_other_short.gguf, wellness_other_short.gguf 모델용
 */
object AiNudgeResponseValidator {

    enum class GroupType {
        CARE,    // 케어 그룹
        HEALTH   // 헬스 그룹
    }

    enum class ActivityLevel {
        INSUFFICIENT,  // 운동 부족
        SUFFICIENT,    // 운동 충분
        UNKNOWN        // 알 수 없음
    }

    enum class TimeOfDay {
        MORNING,    // 아침 (06:00-12:00)
        AFTERNOON,  // 오후 (12:00-18:00)
        EVENING,    // 저녁 (18:00-22:00)
        NIGHT       // 밤 (22:00-06:00)
    }

    /**
     * AI 응답(짧은 메시지) 유효성 검증
     */
    fun isValidResponse(response: String?): Boolean {
        if (response.isNullOrBlank()) return false

        val trimmed = response.trim()

        // 너무 짧거나 긴 응답 (콕 찌르기는 5~30자 정도)
        if (trimmed.length < 3 || trimmed.length > 30) return false

        // 무의미한 패턴 체크
        if (isInvalidPattern(trimmed)) return false

        // 숫자만 나열된 응답 체크
        if (isNumberOnlyResponse(trimmed)) return false

        return true
    }

    private fun isInvalidPattern(text: String): Boolean {
        val invalidPatterns = listOf(
            "\\[출력\\s*없음\\]",
            "\\[.*오류.*\\]",
            "\\[.*에러.*\\]",
            "null",
            "undefined",
            "N/A"
        )

        return invalidPatterns.any { pattern ->
            text.matches(Regex(pattern, RegexOption.IGNORE_CASE))
        }
    }

    private fun isNumberOnlyResponse(text: String): Boolean {
        val numberPattern = "[0-9,\\s]+(?:kcal|bpm|시간|분|km|m|cm|kg|g|보|회|개)".toRegex()
        val matches = numberPattern.findAll(text)
        val numberCharCount = matches.sumOf { it.value.length }
        val ratio = numberCharCount.toDouble() / text.length

        if (ratio > 0.6) return true

        val zeroValuePattern = "0\\s*(?:bpm|kcal|km|시간)".toRegex()
        if (zeroValuePattern.containsMatchIn(text)) return true

        return false
    }

    /**
     * 상황에 맞는 더미 콕 찌르기 메시지 반환
     */
    fun getDummyNudge(
        groupType: GroupType,
        activityLevel: ActivityLevel = ActivityLevel.UNKNOWN,
        timeOfDay: TimeOfDay? = null
    ): String {
        val time = timeOfDay ?: getCurrentTimeOfDay()

        val nudges = when (groupType) {
            GroupType.CARE -> getCareNudges(time, activityLevel)
            GroupType.HEALTH -> getHealthNudges(time, activityLevel)
        }

        return nudges[Random.nextInt(nudges.size)]
    }

    private fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).hour
        return when (hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * 케어 그룹 콕 찌르기 메시지 (짧은 격려)
     */
    private fun getCareNudges(time: TimeOfDay, activityLevel: ActivityLevel): List<String> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "오늘은 움직여봐요!",
                    "산책 어때요?",
                    "가볍게 스트레칭!",
                    "몸 좀 풀어요",
                    "활동량 늘려봐요"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "좋은 아침!",
                    "오늘도 화이팅!",
                    "잘하고 있어요!",
                    "멋져요!",
                    "최고예요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "좋은 아침!",
                    "오늘도 화이팅!",
                    "건강한 하루!",
                    "힘내요!",
                    "응원해요!"
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "잠깐 걸어봐요",
                    "스트레칭 타임!",
                    "조금만 움직여요",
                    "활동량 채워요",
                    "움직일 시간!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오후도 활기차요!",
                    "잘하고 있어요!",
                    "계속 화이팅!",
                    "멋져요!",
                    "대단해요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "좋은 오후!",
                    "힘내요!",
                    "화이팅!",
                    "건강하게!",
                    "응원해요!"
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "저녁 산책 어때요?",
                    "조금만 더 움직여요",
                    "스트레칭 해요",
                    "몸 좀 풀어요",
                    "마무리 운동!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘도 수고했어요!",
                    "잘했어요!",
                    "최고예요!",
                    "멋진 하루!",
                    "푹 쉬세요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "수고했어요!",
                    "편안한 저녁!",
                    "잘 쉬어요!",
                    "화이팅!",
                    "내일 또 봐요!"
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "내일은 더 움직여요",
                    "내일 화이팅!",
                    "잘 쉬어요",
                    "내일은 더 활발히!",
                    "굿나잇!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘도 수고!",
                    "잘했어요!",
                    "푹 쉬세요!",
                    "좋은 꿈!",
                    "내일도 화이팅!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "편안한 밤!",
                    "잘 자요!",
                    "굿나잇!",
                    "푹 쉬어요!",
                    "내일 봐요!"
                )
            }
        }
    }

    /**
     * 헬스 그룹 콕 찌르기 메시지 (짧은 격려)
     */
    private fun getHealthNudges(time: TimeOfDay, activityLevel: ActivityLevel): List<String> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "운동 시작해요!",
                    "목표 달성해요!",
                    "운동량 늘려요",
                    "모닝 러닝 가요!",
                    "지금 운동!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "아침부터 대단!",
                    "최고예요!",
                    "멋져요!",
                    "잘하고 있어요!",
                    "오늘도 화이팅!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "운동 시작!",
                    "목표 세워요!",
                    "오늘도 화이팅!",
                    "힘내요!",
                    "파이팅!"
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "운동할 시간!",
                    "목표까지 조금만!",
                    "지금 운동해요",
                    "움직여요!",
                    "운동량 채워요"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "끈기 대단해요!",
                    "잘하고 있어요!",
                    "조금만 더!",
                    "거의 다 왔어요!",
                    "화이팅!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "운동 타임!",
                    "목표 향해!",
                    "화이팅!",
                    "힘내요!",
                    "파이팅!"
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "마지막 스퍼트!",
                    "목표까지 조금만!",
                    "끝까지 화이팅!",
                    "지금이라도 운동!",
                    "포기하지 마요!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "목표 달성!",
                    "완벽해요!",
                    "수고했어요!",
                    "최고예요!",
                    "잘했어요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "마무리 잘해요!",
                    "수고했어요!",
                    "화이팅!",
                    "잘 쉬어요!",
                    "내일 또 봐요!"
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "내일은 꼭!",
                    "내일 목표 달성!",
                    "잘 쉬어요",
                    "내일 화이팅!",
                    "푹 쉬고 도전!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "목표 완수!",
                    "정말 대단해요!",
                    "수고했어요!",
                    "푹 쉬세요!",
                    "내일도 화이팅!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "수고했어요!",
                    "잘 자요!",
                    "굿나잇!",
                    "푹 쉬어요!",
                    "내일 봐요!"
                )
            }
        }
    }
}
