package com.a307.linkcare.common.util.validator

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * AI 편지쓰기 메시지 (3문장) 검증 및 더미 데이터 제공
 * health_other.gguf, wellness_other.gguf 모델용
 */
object AiLetterResponseValidator {

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
     * AI 응답(3문장 리스트) 유효성 검증
     */
    fun isValidResponse(response: List<String>?): Boolean {
        if (response.isNullOrEmpty()) return false
        if (response.size != 3) return false

        return response.all { sentence ->
            sentence.isNotBlank() &&
            sentence.length >= 5 &&
            sentence.length <= 100 &&
            !isInvalidPattern(sentence) &&
            !isNumberOnlyResponse(sentence)
        }
    }

    /**
     * 단일 문자열 응답 검증
     */
    fun isValidSingleResponse(response: String?): Boolean {
        if (response.isNullOrBlank()) return false

        val trimmed = response.trim()

        if (isInvalidPattern(trimmed)) return false
        if (isNumberOnlyResponse(trimmed)) return false
        if (trimmed.length < 15 || trimmed.length > 300) return false

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
     * 상황에 맞는 더미 편지 (3문장) 반환
     */
    fun getDummyLetter(
        groupType: GroupType,
        activityLevel: ActivityLevel = ActivityLevel.UNKNOWN,
        timeOfDay: TimeOfDay? = null
    ): List<String> {
        val time = timeOfDay ?: getCurrentTimeOfDay()

        val letters = when (groupType) {
            GroupType.CARE -> getCareLetters(time, activityLevel)
            GroupType.HEALTH -> getHealthLetters(time, activityLevel)
        }

        return letters[Random.nextInt(letters.size)]
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
     * 케어 그룹 편지 (3문장)
     */
    private fun getCareLetters(time: TimeOfDay, activityLevel: ActivityLevel): List<List<String>> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "좋은 아침이에요!",
                        "오늘은 가벼운 산책으로 하루를 시작해보는 건 어떨까요?",
                        "작은 움직임이 큰 변화를 만들어요, 함께 화이팅!"
                    ),
                    listOf(
                        "상쾌한 아침이네요.",
                        "오늘은 조금 더 활동적으로 보내보면 좋을 것 같아요.",
                        "당신의 건강을 응원합니다!"
                    ),
                    listOf(
                        "새로운 하루가 시작됐어요.",
                        "아침 스트레칭으로 몸을 깨워보는 건 어떨까요?",
                        "건강한 하루 만들어가요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "아침부터 활동적이시네요!",
                        "이런 좋은 습관이 건강을 지켜줘요.",
                        "오늘도 멋진 하루 보내세요!"
                    ),
                    listOf(
                        "건강한 아침 루틴이 보기 좋아요.",
                        "꾸준한 활동이 정말 대단해요.",
                        "계속 응원할게요!"
                    ),
                    listOf(
                        "아침부터 에너지가 넘치시네요.",
                        "이대로만 유지하시면 완벽해요.",
                        "오늘도 화이팅!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "좋은 아침이에요!",
                        "오늘 하루도 건강하게 보내세요.",
                        "언제나 응원하고 있어요."
                    ),
                    listOf(
                        "상쾌한 아침이네요.",
                        "행복한 하루 되시길 바라요.",
                        "함께 힘내요!"
                    )
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "오후 시간이 왔어요.",
                        "잠깐의 스트레칭이나 걷기도 좋은 운동이 돼요.",
                        "함께 건강 챙겨봐요!"
                    ),
                    listOf(
                        "점심 식사 후 가벼운 산책 어떠세요?",
                        "오후 활동이 건강에 큰 도움이 돼요.",
                        "조금씩 움직여봐요!"
                    ),
                    listOf(
                        "오후 슬럼프가 올 시간이네요.",
                        "가벼운 움직임으로 활력을 되찾아봐요.",
                        "당신을 응원해요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오후에도 활기차시네요!",
                        "꾸준한 활동 정말 멋져요.",
                        "이대로 계속 화이팅!"
                    ),
                    listOf(
                        "활동적인 모습 보기 좋아요.",
                        "건강한 습관을 잘 유지하고 계시네요.",
                        "정말 대단해요!"
                    ),
                    listOf(
                        "오후에도 에너지 넘치시네요.",
                        "이런 끈기가 건강을 만들어요.",
                        "계속 응원할게요!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "따뜻한 오후 시간이네요.",
                        "편안하게 보내고 계신가요?",
                        "건강한 하루 되세요."
                    ),
                    listOf(
                        "오후 시간 잘 보내고 계시죠?",
                        "여유로운 시간 되시길 바라요.",
                        "언제나 함께해요!"
                    )
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "저녁 시간이 왔어요.",
                        "오늘 하루 조금 더 움직여보는 건 어떨까요?",
                        "가벼운 산책으로 하루를 마무리해봐요."
                    ),
                    listOf(
                        "하루를 마무리하는 시간이에요.",
                        "저녁 스트레칭으로 몸을 풀어주세요.",
                        "내일은 더 건강한 하루 만들어요!"
                    ),
                    listOf(
                        "저녁이 되었네요.",
                        "오늘은 운동이 조금 부족했어요.",
                        "내일은 함께 더 활동적으로 보내봐요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오늘 하루 정말 잘 보냈어요!",
                        "활동적인 하루였네요.",
                        "푹 쉬고 내일도 화이팅!"
                    ),
                    listOf(
                        "오늘도 건강하게 보냈어요.",
                        "이런 꾸준함이 정말 대단해요.",
                        "편안한 저녁 되세요!"
                    ),
                    listOf(
                        "활발한 하루를 보내셨네요.",
                        "이런 노력이 건강을 지켜줘요.",
                        "오늘도 수고 많으셨어요!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "저녁 시간이네요.",
                        "오늘 하루 수고 많으셨어요.",
                        "편안한 저녁 보내세요."
                    ),
                    listOf(
                        "하루 마무리 잘하고 계시죠?",
                        "따뜻한 저녁 되시길 바라요.",
                        "내일 또 만나요!"
                    )
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "오늘 하루 활동이 부족했어요.",
                        "내일은 조금 더 움직여봐요.",
                        "푹 쉬고 내일 화이팅!"
                    ),
                    listOf(
                        "오늘은 운동이 부족했네요.",
                        "충분히 휴식 취하고 내일은 더 활동적으로!",
                        "당신을 응원합니다!"
                    ),
                    listOf(
                        "오늘은 조금 아쉬웠어요.",
                        "하지만 내일이 있잖아요.",
                        "잘 쉬고 내일 다시 시작해요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오늘도 활동적으로 보냈어요!",
                        "정말 잘하셨어요.",
                        "편안한 밤 되세요!"
                    ),
                    listOf(
                        "오늘 하루 정말 수고했어요.",
                        "건강한 생활 습관이 보기 좋아요.",
                        "좋은 꿈 꾸세요!"
                    ),
                    listOf(
                        "활발한 하루를 보내셨네요.",
                        "이런 노력이 멋져요.",
                        "푹 쉬고 내일도 화이팅!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "오늘 하루 수고했어요.",
                        "푹 쉬고 내일 또 만나요.",
                        "편안한 밤 되세요!"
                    ),
                    listOf(
                        "하루 마무리 시간이네요.",
                        "충분히 휴식 취하세요.",
                        "좋은 꿈 꾸세요!"
                    )
                )
            }
        }
    }

    /**
     * 헬스 그룹 편지 (3문장)
     */
    private fun getHealthLetters(time: TimeOfDay, activityLevel: ActivityLevel): List<List<String>> {
        return when (time) {
            TimeOfDay.MORNING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "좋은 아침이에요!",
                        "오늘은 운동 목표를 달성해봐요.",
                        "아침 운동으로 활기차게 시작해볼까요?"
                    ),
                    listOf(
                        "새로운 아침이에요.",
                        "모닝 루틴에 운동을 추가해보는 건 어떨까요?",
                        "함께 목표를 향해 달려가요!"
                    ),
                    listOf(
                        "상쾌한 아침이네요.",
                        "오늘은 운동량을 늘려봐요.",
                        "당신의 도전을 응원합니다!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "아침부터 운동하시다니 대단해요!",
                        "이런 의지가 목표 달성의 비결이에요.",
                        "오늘도 최고예요!"
                    ),
                    listOf(
                        "모닝 운동 정말 멋져요!",
                        "꾸준한 실천이 결과를 만들어요.",
                        "계속 화이팅!"
                    ),
                    listOf(
                        "아침부터 활력이 넘치시네요!",
                        "이런 끈기가 성공을 만들어요.",
                        "정말 대단해요!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "좋은 아침이에요!",
                        "오늘의 운동 목표를 세워봐요.",
                        "함께 달성해나가요!"
                    ),
                    listOf(
                        "상쾌한 아침이네요.",
                        "오늘도 힘차게 시작해봐요.",
                        "당신을 응원합니다!"
                    )
                )
            }
            TimeOfDay.AFTERNOON -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "오후 운동 시간이에요!",
                        "아직 목표 달성할 시간이 충분해요.",
                        "지금 시작해봐요!"
                    ),
                    listOf(
                        "점심 후 가벼운 운동 어때요?",
                        "조금씩 움직이면 목표에 가까워져요.",
                        "함께 힘내요!"
                    ),
                    listOf(
                        "오후에 운동하면 딱 좋아요.",
                        "운동량을 늘려봐요.",
                        "당신의 노력을 응원해요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오후에도 꾸준하시네요!",
                        "이런 끈기가 정말 멋져요.",
                        "목표까지 조금만 더!"
                    ),
                    listOf(
                        "활동량이 정말 좋아요.",
                        "이대로만 유지하면 완벽해요.",
                        "계속 응원할게요!"
                    ),
                    listOf(
                        "오후에도 열심히 하시네요.",
                        "이런 노력이 대단해요.",
                        "정말 잘하고 있어요!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "오후 운동 타임이에요.",
                        "오늘의 목표를 향해 가봐요.",
                        "화이팅!"
                    ),
                    listOf(
                        "따뜻한 오후네요.",
                        "운동하기 좋은 시간이에요.",
                        "함께 힘내요!"
                    )
                )
            }
            TimeOfDay.EVENING -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "저녁이에요, 지금이라도 운동해봐요.",
                        "오늘 목표 달성까지 조금만 더!",
                        "끝까지 함께해요!"
                    ),
                    listOf(
                        "저녁 운동으로 하루를 완성해봐요.",
                        "포기하지 않는 당신이 멋져요.",
                        "마지막까지 화이팅!"
                    ),
                    listOf(
                        "하루를 마무리할 시간이에요.",
                        "운동량을 조금 더 채워봐요.",
                        "당신의 노력을 응원해요!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오늘 운동 목표 달성했어요!",
                        "정말 잘하셨어요, 축하해요!",
                        "푹 쉬고 내일도 화이팅!"
                    ),
                    listOf(
                        "오늘도 완벽한 하루였어요.",
                        "꾸준한 노력이 빛을 발하고 있어요.",
                        "정말 대단해요!"
                    ),
                    listOf(
                        "목표를 달성하셨네요!",
                        "이런 끈기가 성공을 만들어요.",
                        "오늘도 수고 많으셨어요!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "저녁 시간이네요.",
                        "오늘 하루 운동은 어땠나요?",
                        "편안한 저녁 되세요."
                    ),
                    listOf(
                        "하루 마무리 잘하고 계시죠?",
                        "운동 목표 점검해봐요.",
                        "내일 또 만나요!"
                    )
                )
            }
            TimeOfDay.NIGHT -> when (activityLevel) {
                ActivityLevel.INSUFFICIENT -> listOf(
                    listOf(
                        "오늘 목표에 조금 못 미쳤어요.",
                        "내일은 꼭 달성해봐요.",
                        "푹 쉬고 내일 화이팅!"
                    ),
                    listOf(
                        "오늘은 조금 아쉬웠어요.",
                        "하지만 내일이 있잖아요.",
                        "충분히 휴식하고 내일 도전해요!"
                    ),
                    listOf(
                        "오늘 운동량이 부족했네요.",
                        "내일은 더 열심히 해봐요.",
                        "당신을 응원합니다!"
                    )
                )
                ActivityLevel.SUFFICIENT -> listOf(
                    listOf(
                        "오늘 운동 목표 완수했어요!",
                        "정말 훌륭해요, 최고예요!",
                        "편안한 밤 되세요!"
                    ),
                    listOf(
                        "오늘도 열심히 하셨어요.",
                        "이런 꾸준함이 성공의 비결이에요.",
                        "좋은 꿈 꾸세요!"
                    ),
                    listOf(
                        "목표 달성 축하해요!",
                        "당신의 노력이 빛나는 하루였어요.",
                        "푹 쉬고 내일도 화이팅!"
                    )
                )
                ActivityLevel.UNKNOWN -> listOf(
                    listOf(
                        "오늘 하루 수고했어요.",
                        "내일 운동 계획 세워봐요.",
                        "편안한 밤 되세요!"
                    ),
                    listOf(
                        "하루 마무리 시간이네요.",
                        "충분히 휴식 취하세요.",
                        "좋은 꿈 꾸세요!"
                    )
                )
            }
        }
    }
}
