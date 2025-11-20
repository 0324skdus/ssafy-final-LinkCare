package com.a307.linkcare.feature.ai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.common.util.AiLetterResponseValidator
import com.a307.linkcare.common.util.AiNudgeResponseValidator
import com.a307.linkcare.feature.ai.domain.service.AiCommentService
import com.a307.linkcare.feature.commongroup.domain.GoalCriteria
import com.a307.linkcare.feature.notification.data.NotificationApi
import com.a307.linkcare.feature.notification.domain.SaveNotificationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 제안 ViewModel
 * - ③ health-other: 케어 그룹 멤버 격려 메시지
 * - ④ wellness-other: 헬스 그룹 멤버 격려 메시지
 * - ⑤ health_other_short: 빠른 넛지 메시지
 */
@HiltViewModel
class AiSuggestionViewModel @Inject constructor(
    private val aiCommentService: AiCommentService,
    private val notificationApi: NotificationApi
) : ViewModel() {

    private val tag = "AiSuggestionViewModel"

    // ③ 케어 그룹 제안 (3문장)
    private val _careGroupSuggestions = MutableStateFlow<List<String>>(emptyList())
    val careGroupSuggestions: StateFlow<List<String>> = _careGroupSuggestions.asStateFlow()

    // ④ 헬스 그룹 제안 (3문장)
    private val _healthGroupSuggestions = MutableStateFlow<List<String>>(emptyList())
    val healthGroupSuggestions: StateFlow<List<String>> = _healthGroupSuggestions.asStateFlow()

    // ⑤ 빠른 넛지 메시지
    private val _quickNudgeMessage = MutableStateFlow<String>("")
    val quickNudgeMessage: StateFlow<String> = _quickNudgeMessage.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ⑤⑥ 콕 찌르기 메모리 캐시 (앱 세션 동안만 유지)
    // Key: "care_$userSeq" or "wellness_$userSeq", Value: (messages: List<String>, currentIndex: Int)
    private val nudgeCache = mutableMapOf<String, Pair<List<String>, Int>>()

    // 현재 로드된 사용자 추적 (앱 세션 동안 유지)
    private var currentCareUserSeq: Long? = null
    private var currentHealthUserSeq: Long? = null

    /**
     * 케어 그룹 캐시 확인
     */
    fun hasCareGroupCache(userSeq: Long): Boolean {
        return currentCareUserSeq == userSeq && _careGroupSuggestions.value.isNotEmpty()
    }

    /**
     * 헬스 그룹 캐시 확인
     */
    fun hasHealthGroupCache(userSeq: Long): Boolean {
        return currentHealthUserSeq == userSeq && _healthGroupSuggestions.value.isNotEmpty()
    }

    /**
     * ③ health-other: 케어 그룹 멤버 격려 메시지 로드 - 더미 모드 (4초 로딩)
     */
    fun loadCareGroupSuggestions(userSeq: Long) {
        viewModelScope.launch {
            // 같은 사용자면 이미 캐싱된 데이터 재사용
            if (currentCareUserSeq == userSeq && _careGroupSuggestions.value.isNotEmpty()) {
                Log.d(tag, "[CARE_SUGGESTIONS] 💾 캐시 재사용: userSeq=$userSeq")
                return@launch
            }

            // 다른 사용자면 캐시 클리어
            if (currentCareUserSeq != userSeq) {
                _careGroupSuggestions.value = emptyList()
                currentCareUserSeq = userSeq
            }

            _isLoading.value = true

            // 현재 시간 확인 (한국 시간대)
            val currentTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
            val currentHour = currentTime.hour
            val timeOfDay = when (currentHour) {
                in 6..11 -> "MORNING"
                in 12..17 -> "AFTERNOON"
                in 18..21 -> "EVENING"
                else -> "NIGHT"
            }

            Log.d(tag, "[CARE_SUGGESTIONS] 💬 더미 데이터 생성 시작 (4초 로딩): userSeq=$userSeq")
            Log.d(tag, "[CARE_SUGGESTIONS] ⏰ 한국 시간: ${currentHour}시, 시간대: $timeOfDay (전체: ${currentTime.toLocalTime()})")

            // 4초 로딩 시뮬레이션
            kotlinx.coroutines.delay(4000)

            // 더미 편지 데이터 3문장 생성
            val dummyLetter = AiLetterResponseValidator.getDummyLetter(
                groupType = AiLetterResponseValidator.GroupType.CARE,
                activityLevel = AiLetterResponseValidator.ActivityLevel.UNKNOWN
            )

            _careGroupSuggestions.value = dummyLetter
            Log.d(tag, "[CARE_SUGGESTIONS] ✅ 더미 데이터 생성 완료: ${dummyLetter.size}개 문장")
            dummyLetter.forEachIndexed { index, text ->
                Log.d(tag, "[CARE_SUGGESTIONS]   ${index + 1}. $text")
            }

            _isLoading.value = false
        }
    }

    /**
     * ④ wellness-other: 헬스 그룹 멤버 격려 메시지 로드 - 더미 모드 (4초 로딩)
     */
    fun loadHealthGroupSuggestions(userSeq: Long, groupSeq: Long) {
        viewModelScope.launch {
            // 같은 사용자면 이미 캐싱된 데이터 재사용
            if (currentHealthUserSeq == userSeq && _healthGroupSuggestions.value.isNotEmpty()) {
                Log.d(tag, "[HEALTH_SUGGESTIONS] 💾 캐시 재사용: userSeq=$userSeq")
                return@launch
            }

            // 다른 사용자면 캐시 클리어
            if (currentHealthUserSeq != userSeq) {
                _healthGroupSuggestions.value = emptyList()
                currentHealthUserSeq = userSeq
            }

            _isLoading.value = true

            // 현재 시간 확인 (한국 시간대)
            val currentTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
            val currentHour = currentTime.hour
            val timeOfDay = when (currentHour) {
                in 6..11 -> "MORNING"
                in 12..17 -> "AFTERNOON"
                in 18..21 -> "EVENING"
                else -> "NIGHT"
            }

            Log.d(tag, "[HEALTH_SUGGESTIONS] 💬 더미 데이터 생성 시작 (4초 로딩): userSeq=$userSeq, groupSeq=$groupSeq")
            Log.d(tag, "[HEALTH_SUGGESTIONS] ⏰ 한국 시간: ${currentHour}시, 시간대: $timeOfDay (전체: ${currentTime.toLocalTime()})")

            // 4초 로딩 시뮬레이션
            kotlinx.coroutines.delay(4000)

            // 더미 편지 데이터 3문장 생성
            val dummyLetter = AiLetterResponseValidator.getDummyLetter(
                groupType = AiLetterResponseValidator.GroupType.HEALTH,
                activityLevel = AiLetterResponseValidator.ActivityLevel.UNKNOWN
            )

            _healthGroupSuggestions.value = dummyLetter
            Log.d(tag, "[HEALTH_SUGGESTIONS] ✅ 더미 데이터 생성 완료: ${dummyLetter.size}개 문장")
            dummyLetter.forEachIndexed { index, text ->
                Log.d(tag, "[HEALTH_SUGGESTIONS]   ${index + 1}. $text")
            }

            _isLoading.value = false
        }
    }

    /**
     * ⑤ health_other_short: 빠른 넛지 메시지 로드 (케어 그룹) - 더미 모드
     * 앱 세션 동안 캐싱: 첫 요청만 더미 생성, 이후 순차 출력
     * - 더미 데이터로 10개의 문장 생성 및 캐싱
     * @param groupSeq 그룹 ID (같은 유저라도 다른 그룹이면 별도 캐시)
     */
    fun loadQuickNudgeMessage(groupSeq: Long, userSeq: Long) {
        viewModelScope.launch {
            val cacheKey = "care_${groupSeq}_$userSeq"

            // 캐시 확인
            val cached = nudgeCache[cacheKey]
            if (cached != null) {
                // 캐시된 데이터 사용 - 순차적으로 다음 문장 반환
                val (messages, currentIndex) = cached
                if (messages.isNotEmpty()) {
                    val nextIndex = (currentIndex + 1) % messages.size
                    val message = messages[currentIndex]

                    // 인덱스 업데이트
                    nudgeCache[cacheKey] = messages to nextIndex

                    _quickNudgeMessage.value = message
                    Log.d(tag, "[QUICK_NUDGE] 💾 캐시 사용: [$currentIndex/${messages.size}] $message")

                    // 서버에 알림 저장
                    sendPokeNotification(userSeq, groupSeq, message)
                    return@launch
                }
            }

            // 캐시 없음 - 더미 데이터 10개 생성
            Log.d(tag, "[QUICK_NUDGE] 💬 더미 데이터 10개 생성 시작: userSeq=$userSeq")

            // 더미 데이터 10개 생성 (중복 허용, 최대 20회 시도)
            val dummyMessages = mutableListOf<String>()
            val dummySet = mutableSetOf<String>()
            var attempts = 0
            val maxAttempts = 20

            while (dummyMessages.size < 10 && attempts < maxAttempts) {
                val dummy = AiNudgeResponseValidator.getDummyNudge(
                    groupType = AiNudgeResponseValidator.GroupType.CARE,
                    activityLevel = AiNudgeResponseValidator.ActivityLevel.UNKNOWN
                )
                if (dummySet.add(dummy)) { // 중복 체크
                    dummyMessages.add(dummy)
                }
                attempts++
            }

            // 10개 미만이면 중복 허용으로 채우기
            while (dummyMessages.size < 10) {
                val dummy = AiNudgeResponseValidator.getDummyNudge(
                    groupType = AiNudgeResponseValidator.GroupType.CARE,
                    activityLevel = AiNudgeResponseValidator.ActivityLevel.UNKNOWN
                )
                dummyMessages.add(dummy)
            }

            // 첫 번째 문장 반환
            val firstMessage = dummyMessages[0]
            _quickNudgeMessage.value = firstMessage

            // 캐시 저장
            nudgeCache[cacheKey] = dummyMessages to 1

            Log.d(tag, "[QUICK_NUDGE] ✅ 더미 데이터 캐싱: 총 ${dummyMessages.size}개 문장")
            dummyMessages.forEachIndexed { index, msg ->
                Log.d(tag, "[QUICK_NUDGE]   ${index + 1}. $msg")
            }

            // 서버에 알림 저장
            sendPokeNotification(userSeq, groupSeq, firstMessage)
        }
    }

    /**
     * ⑥ wellness_other_short: 빠른 넛지 메시지 로드 (헬스 그룹) - 더미 모드
     * 앱 세션 동안 캐싱: 첫 요청만 더미 생성, 이후 순차 출력
     * - 더미 데이터로 10개의 문장 생성 및 캐싱
     * @param groupSeq 그룹 ID (같은 유저라도 다른 그룹이면 별도 캐시)
     */
    fun loadQuickWellnessNudgeMessage(groupSeq: Long, userSeq: Long) {
        viewModelScope.launch {
            val cacheKey = "wellness_${groupSeq}_$userSeq"

            // 캐시 확인
            val cached = nudgeCache[cacheKey]
            if (cached != null) {
                // 캐시된 데이터 사용 - 순차적으로 다음 문장 반환
                val (messages, currentIndex) = cached
                if (messages.isNotEmpty()) {
                    val nextIndex = (currentIndex + 1) % messages.size
                    val message = messages[currentIndex]

                    // 인덱스 업데이트
                    nudgeCache[cacheKey] = messages to nextIndex

                    _quickNudgeMessage.value = message
                    Log.d(tag, "[QUICK_WELLNESS_NUDGE] 💾 캐시 사용: [$currentIndex/${messages.size}] $message")

                    // 서버에 알림 저장
                    sendPokeNotification(userSeq, groupSeq, message)
                    return@launch
                }
            }

            // 캐시 없음 - 더미 데이터 10개 생성
            Log.d(tag, "[QUICK_WELLNESS_NUDGE] 💬 더미 데이터 10개 생성 시작: userSeq=$userSeq, groupSeq=$groupSeq")

            // 더미 데이터 10개 생성 (중복 허용, 최대 20회 시도)
            val dummyMessages = mutableListOf<String>()
            val dummySet = mutableSetOf<String>()
            var attempts = 0
            val maxAttempts = 20

            while (dummyMessages.size < 10 && attempts < maxAttempts) {
                val dummy = AiNudgeResponseValidator.getDummyNudge(
                    groupType = AiNudgeResponseValidator.GroupType.HEALTH,
                    activityLevel = AiNudgeResponseValidator.ActivityLevel.UNKNOWN
                )
                if (dummySet.add(dummy)) { // 중복 체크
                    dummyMessages.add(dummy)
                }
                attempts++
            }

            // 10개 미만이면 중복 허용으로 채우기
            while (dummyMessages.size < 10) {
                val dummy = AiNudgeResponseValidator.getDummyNudge(
                    groupType = AiNudgeResponseValidator.GroupType.HEALTH,
                    activityLevel = AiNudgeResponseValidator.ActivityLevel.UNKNOWN
                )
                dummyMessages.add(dummy)
            }

            // 첫 번째 문장 반환
            val firstMessage = dummyMessages[0]
            _quickNudgeMessage.value = firstMessage

            // 캐시 저장
            nudgeCache[cacheKey] = dummyMessages to 1

            Log.d(tag, "[QUICK_WELLNESS_NUDGE] ✅ 더미 데이터 캐싱: 총 ${dummyMessages.size}개 문장")
            dummyMessages.forEachIndexed { index, msg ->
                Log.d(tag, "[QUICK_WELLNESS_NUDGE]   ${index + 1}. $msg")
            }

            // 서버에 알림 저장
            sendPokeNotification(userSeq, groupSeq, firstMessage)
        }
    }

    /**
     * 콕 찌르기 알림을 서버에 전송
     */
    private fun sendPokeNotification(receiverUserPk: Long, groupSeq: Long, content: String) {
        viewModelScope.launch {
            try {
                val request = SaveNotificationRequest(
                    receiverUserPk = receiverUserPk,
                    groupSeq = groupSeq,
                    messageType = "POKE",
                    content = content
                )

                val response = notificationApi.saveNotification(request)

                if (response.isSuccessful) {
                    Log.d(tag, "[POKE_NOTIFICATION] ✅ 서버 전송 성공: $content")
                } else {
                    Log.e(tag, "[POKE_NOTIFICATION] ❌ 서버 전송 실패: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "[POKE_NOTIFICATION] ❌ 서버 전송 예외: ${e.message}", e)
            }
        }
    }

    /**
     * 편지 알림을 서버에 전송
     */
    suspend fun sendLetterNotification(receiverUserPk: Long, groupSeq: Long, content: String): Result<Unit> {
        try {
            val request = SaveNotificationRequest(
                receiverUserPk = receiverUserPk,
                groupSeq = groupSeq,
                messageType = "LETTER",
                content = content
            )

            val response = notificationApi.saveNotification(request)

            if (response.isSuccessful) {
                Log.d(tag, "[LETTER_NOTIFICATION] ✅ 편지 전송 성공: $content")
                return Result.success(Unit)
            } else {
                Log.e(tag, "[LETTER_NOTIFICATION] ❌ 편지 전송 실패: ${response.code()} - ${response.message()}")
                return Result.failure(Exception("편지 전송 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "[LETTER_NOTIFICATION] ❌ 편지 전송 예외: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * 상태 초기화
     */
    fun clearSuggestions() {
        _careGroupSuggestions.value = emptyList()
        _healthGroupSuggestions.value = emptyList()
        _quickNudgeMessage.value = ""
    }

    // ========== Private Helper Functions ==========

    /**
     * AI 응답을 문장 단위로 분리
     * 예: "오늘도 힘차게!\n걷는 즐거움!" → ["오늘도 힘차게!", "걷는 즐거움!"]
     */
    private fun parseSentences(text: String): List<String> {
        // 1. 줄바꿈으로 먼저 분리
        val lines = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val sentences = mutableListOf<String>()

        // 2. 각 줄을 마침표, 느낌표, 물음표로 추가 분리
        for (line in lines) {
            // 문장 부호로 분리 (부호 포함하여 분리)
            val parts = line.split(Regex("(?<=[.!?])\\s*"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            sentences.addAll(parts)
        }

        return sentences
    }

    /**
     * 목표 개수만큼 문장 확보
     * - 유효한 문장이 부족하면 더미 데이터로 채움
     * @param validMessages 검증된 유효한 문장 리스트
     * @param groupType 그룹 타입 (CARE 또는 HEALTH)
     * @param targetCount 목표 문장 개수 (기본 10개)
     */
    private fun ensureTenMessages(
        validMessages: List<String>,
        groupType: AiNudgeResponseValidator.GroupType,
        targetCount: Int = 10
    ): List<String> {
        val result = validMessages.toMutableList()

        // 이미 목표 개수 이상이면 목표 개수만큼만 반환
        if (result.size >= targetCount) {
            return result.take(targetCount)
        }

        // 부족한 만큼 더미 데이터로 채우기
        val needCount = targetCount - result.size
        Log.d(tag, "[ENSURE_TEN] 부족한 문장: ${needCount}개, 더미로 채움")

        // 활동 수준은 UNKNOWN으로 (일반적인 격려 메시지)
        val activityLevel = AiNudgeResponseValidator.ActivityLevel.UNKNOWN

        // 중복되지 않도록 더미 메시지를 충분히 생성
        val dummyPool = mutableSetOf<String>()
        while (dummyPool.size < needCount * 2) { // 여유있게 2배 생성
            val dummy = AiNudgeResponseValidator.getDummyNudge(
                groupType = groupType,
                activityLevel = activityLevel
            )
            dummyPool.add(dummy)
        }

        // 이미 있는 문장과 중복되지 않는 더미 선택
        val availableDummies = dummyPool.filter { it !in result }
        result.addAll(availableDummies.take(needCount))

        Log.d(tag, "[ENSURE_TEN] 최종 문장 수: ${result.size}개 (유효: ${validMessages.size}, 더미: ${result.size - validMessages.size})")

        return result
    }
}
