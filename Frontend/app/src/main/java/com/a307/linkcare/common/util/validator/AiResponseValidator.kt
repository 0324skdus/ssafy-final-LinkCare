package com.a307.linkcare.common.util.validator

import java.time.LocalTime
import kotlin.random.Random

/**
 * AI 응답 검증 및 상황별 더미 데이터 제공 유틸리티
 */
object AiResponseValidator {

    /**
     * 그룹 타입
     */
    enum class GroupType {
        CARE,    // 케어 그룹
        HEALTH   // 헬스 그룹
    }

    /**
     * 운동량 상태
     */
    enum class ActivityLevel {
        INSUFFICIENT,  // 운동 부족
        SUFFICIENT,    // 운동 충분
        UNKNOWN        // 알 수 없음
    }

    /**
     * 시간대
     */
    enum class TimeOfDay {
        MORNING,    // 아침 (06:00-12:00)
        AFTERNOON,  // 오후 (12:00-18:00)
        EVENING,    // 저녁 (18:00-22:00)
        NIGHT       // 밤 (22:00-06:00)
    }

    /**
     * AI 응답이 유효한지 검증
     */
    fun isValidResponse(response: String?): Boolean {
        if (response.isNullOrBlank()) return false

        val trimmed = response.trim()

        // 1. 너무 짧은 응답 (3자 미만)
        if (trimmed.length < 3) return false

        // 2. 무의미한 패턴 감지
        val invalidPatterns = listOf(
            "\\[출력\\s*없음\\]",
            "\\[.*오류.*\\]",
            "\\[.*에러.*\\]",
            "null",
            "undefined",
            "N/A",
            "n/a",
            "응답\\s*없음",
            "데이터\\s*없음"
        )

        for (pattern in invalidPatterns) {
            if (trimmed.matches(Regex(pattern, RegexOption.IGNORE_CASE))) {
                return false
            }
        }

        // 3. 숫자 데이터만 나열된 경우 감지
        if (isNumberOnlyResponse(trimmed)) {
            return false
        }

        // 4. 너무 긴 응답 (200자 초과)
        if (trimmed.length > 200) return false

        return true
    }

    /**
     * 숫자 데이터만 나열된 응답인지 확인
     */
    private fun isNumberOnlyResponse(text: String): Boolean {
        // 숫자, 단위, 쉼표, 공백이 전체의 60% 이상을 차지하면 숫자 데이터로 간주
        val numberPattern = "[0-9,\\s]+(?:kcal|bpm|시간|분|km|m|cm|kg|g|면|회|개)".toRegex()
        val matches = numberPattern.findAll(text)
        val numberCharCount = matches.sumOf { it.value.length }

        val ratio = numberCharCount.toDouble() / text.length

        // 60% 이상이 숫자 데이터면 무효
        if (ratio > 0.6) return true

        // 또는 "0bpm", "0kcal" 같은 명백히 잘못된 값이 있으면 무효
        val zeroValuePattern = "0\\s*(?:bpm|kcal|km|시간)".toRegex()
        if (zeroValuePattern.containsMatchIn(text)) return true

        return false
    }

    /**
     * 상황에 맞는 더미 응답 반환
     *
     * @param groupType 그룹 타입 (CARE/HEALTH)
     * @param activityLevel 운동량 상태
     * @param timeOfDay 시간대 (null이면 현재 시간 기준)
     */
    fun getDummyResponse(
        groupType: GroupType,
        activityLevel: ActivityLevel = ActivityLevel.UNKNOWN,
        timeOfDay: TimeOfDay? = null
    ): String {
        val time = timeOfDay ?: getCurrentTimeOfDay()

        val responses = when (groupType) {
            GroupType.CARE -> getCareResponses(time, activityLevel)
            GroupType.HEALTH -> getHealthResponses(time, activityLevel)
        }

        return responses[Random.nextInt(responses.size)]
    }

    /**
     * 현재 시간대 반환
     */
    private fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * 케어 그룹용 더미 응답
     */
    private fun getCareResponses(time: TimeOfDay, activityLevel: ActivityLevel): List<String> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "좋은 아침이에요! 오늘은 가벼운 스트레칭으로 시작해볼까요?",
                    "아침 햇살처럼 활기찬 하루 되세요! 움직임이 필요해 보여요",
                    "상쾌한 아침이네요! 오늘은 조금 더 활동적으로 보내봐요!",
                    "굿모닝! 아침 산책으로 하루를 시작하면 좋을 것 같아요",
                    "아침이에요! 가볍게 몸을 움직여주면 더 좋을 것 같아요"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "활기찬 아침이네요! 오늘도 건강하게 보내세요!",
                    "좋은 아침! 건강한 하루의 시작이에요!",
                    "상쾌한 아침! 활동적인 모습이 보기 좋아요!",
                    "멋진 아침이에요! 꾸준한 활동 너무 좋아요!",
                    "아침부터 에너지가 넘치네요! 최고예요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "좋은 아침이에요! 오늘 하루도 힘차게!",
                    "상쾌한 아침! 건강한 하루 되세요!",
                    "굿모닝! 행복한 하루 시작하세요!",
                    "아침이에요! 오늘도 화이팅!",
                    "새로운 아침! 좋은 하루 되세요!"
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "오후에는 가벼운 움직임이 필요해요! 잠깐 스트레칭 어때요?",
                    "활동량이 부족해 보여요. 짧은 산책 추천드려요!",
                    "오후 슬럼프를 운동으로 극복해봐요!",
                    "점심 후 가벼운 걷기 운동 어떠세요?",
                    "조금 더 활동적으로 보내면 좋을 것 같아요!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오후에도 활기차네요! 멋져요!",
                    "활동적인 모습 너무 좋아요!",
                    "꾸준한 활동, 정말 대단해요!",
                    "오후에도 에너지 넘치네요!",
                    "활발한 모습이 보기 좋아요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "오후 시간 잘 보내고 계신가요?",
                    "따뜻한 오후네요! 좋은 하루!",
                    "평온한 오후 되세요!",
                    "오후에도 건강하게!",
                    "여유로운 오후 보내세요!"
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "저녁 시간, 가벼운 산책으로 하루를 마무리해봐요!",
                    "오늘 하루 운동이 부족했어요. 저녁 스트레칭 어때요?",
                    "저녁 산책으로 하루를 정리해보세요!",
                    "가벼운 운동으로 하루를 마무리하면 좋아요!",
                    "저녁에 몸을 조금 풀어주면 어떨까요?"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘 하루도 활동적으로 보냈네요! 잘하셨어요!",
                    "활발한 하루였어요! 푹 쉬세요!",
                    "오늘도 건강하게 보냈네요! 수고했어요!",
                    "활동적인 하루! 내일도 화이팅!",
                    "멋진 하루였어요! 편안한 저녁 되세요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "저녁 시간, 편안하게 보내세요!",
                    "하루 마무리 잘하고 계신가요?",
                    "따뜻한 저녁 되세요!",
                    "편안한 저녁 보내세요!",
                    "오늘 하루 수고하셨어요!"
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "오늘 활동이 부족했어요. 내일은 더 활동적으로!",
                    "내일은 조금 더 움직여봐요! 굿나잇!",
                    "푹 쉬고 내일은 더 활발하게 보내요!",
                    "충분한 휴식 후 내일은 활동적으로!",
                    "잘 쉬고 내일 힘차게 시작해요!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘도 활동적으로 보냈네요! 푹 쉬세요!",
                    "멋진 하루였어요! 편안한 밤 되세요!",
                    "오늘도 수고했어요! 좋은 꿈 꾸세요!",
                    "건강한 하루! 내일도 화이팅!",
                    "활발한 하루였어요! 잘 쉬세요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "편안한 밤 되세요! 좋은 꿈 꾸세요!",
                    "푹 쉬고 내일 또 봐요!",
                    "숙면 취하세요! 굿나잇!",
                    "오늘 하루 수고했어요! 잘 자요!",
                    "따뜻한 밤 되세요!"
                )
            }
        }
    }

    /**
     * 헬스 그룹용 더미 응답
     */
    private fun getHealthResponses(time: TimeOfDay, activityLevel: ActivityLevel): List<String> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "아침 운동으로 하루를 시작해봐요!",
                    "모닝 루틴에 운동을 추가해보는 건 어때요?",
                    "아침 스트레칭으로 몸을 깨워봐요!",
                    "오늘은 운동 목표를 달성해봐요!",
                    "아침 러닝으로 활력을 찾아봐요!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "아침부터 운동! 정말 대단해요!",
                    "모닝 운동 완벽해요! 최고예요!",
                    "활기찬 아침 루틴이네요! 멋져요!",
                    "아침 운동 습관 정말 훌륭해요!",
                    "조조 운동! 의지가 대단해요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "아침 운동 준비되셨나요? 화이팅!",
                    "활기찬 아침! 운동하기 좋은 날씨네요!",
                    "오늘의 운동 목표를 세워봐요!",
                    "상쾌한 아침! 몸을 움직여봐요!",
                    "좋은 아침! 오늘도 운동 파이팅!"
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "오후 운동 시간이에요! 움직여봐요!",
                    "점심 후 가벼운 운동 어때요?",
                    "아직 운동할 시간 충분해요!",
                    "오후 러닝으로 활력 충전!",
                    "운동 목표 달성해봐요! 화이팅!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오후에도 운동! 끈기가 대단해요!",
                    "꾸준한 운동 습관 멋져요!",
                    "오후 운동까지! 완벽해요!",
                    "활동량 목표 달성 중이네요!",
                    "열심히 운동하는 모습 최고예요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "오후 운동 타임! 준비됐나요?",
                    "운동하기 좋은 오후네요!",
                    "오늘의 운동 목표는?",
                    "활기찬 오후 보내세요!",
                    "운동으로 오후 슬럼프 극복!"
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "저녁 운동으로 하루를 마무리해봐요!",
                    "오늘 운동량이 부족해요. 지금이라도!",
                    "저녁 러닝으로 목표 달성해봐요!",
                    "운동 목표까지 조금만 더! 화이팅!",
                    "저녁 운동으로 하루를 완성해요!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘 운동 목표 달성! 축하해요!",
                    "완벽한 하루였어요! 수고했어요!",
                    "운동 목표 클리어! 정말 멋져요!",
                    "오늘도 열심히 운동했네요! 최고!",
                    "훌륭한 운동량이에요! 대단해요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "저녁 운동 타임! 마무리 잘해봐요!",
                    "하루 운동 목표 점검 시간이에요!",
                    "저녁 스트레칭으로 마무리!",
                    "오늘 하루 운동 어땠나요?",
                    "편안한 저녁 운동 되세요!"
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    "오늘 운동이 부족했어요. 내일은 더 열심히!",
                    "내일은 운동 목표 꼭 달성해봐요!",
                    "푹 쉬고 내일 운동 화이팅!",
                    "충분한 휴식 후 내일 도전해요!",
                    "내일은 더 활동적으로! 굿나잇!"
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    "오늘 운동 목표 완수! 최고예요!",
                    "완벽한 운동량이었어요! 푹 쉬세요!",
                    "오늘도 열심히! 내일도 화이팅!",
                    "운동 목표 달성! 잘 쉬세요!",
                    "멋진 하루였어요! 좋은 꿈 꾸세요!"
                )
                ActivityLevel.UNKNOWN -> listOf(
                    "오늘 하루 수고했어요! 푹 쉬세요!",
                    "내일 운동 계획 세우고 자요!",
                    "편안한 밤 되세요! 굿나잇!",
                    "충분한 휴식 취하세요!",
                    "잘 쉬고 내일 또 운동해요!"
                )
            }
        }
    }
}
