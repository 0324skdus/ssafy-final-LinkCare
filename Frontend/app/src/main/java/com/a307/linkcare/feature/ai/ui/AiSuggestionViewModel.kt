package com.a307.linkcare.feature.ai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /**
     * ③ health-other: 케어 그룹 멤버 격려 메시지 로드
     */
    fun loadCareGroupSuggestions(userSeq: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(tag, "[CARE_SUGGESTIONS] 로드 시작: userSeq=$userSeq")

            aiCommentService.generateCareGroupMemberEncouragement(userSeq)
                .onSuccess { suggestions ->
                    _careGroupSuggestions.value = suggestions
                    Log.d(tag, "[CARE_SUGGESTIONS] ✅ 성공: ${suggestions.size}개")
                    suggestions.forEachIndexed { index, text ->
                        Log.d(tag, "[CARE_SUGGESTIONS]   ${index + 1}. $text")
                    }
                }
                .onFailure { error ->
                    Log.e(tag, "[CARE_SUGGESTIONS] ❌ 실패: ${error.message}")
                    _careGroupSuggestions.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    /**
     * ④ wellness-other: 헬스 그룹 멤버 격려 메시지 로드
     */
    fun loadHealthGroupSuggestions(userSeq: Long, goalCriteria: GoalCriteria?) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(tag, "[HEALTH_SUGGESTIONS] 로드 시작: userSeq=$userSeq, goal=$goalCriteria")

            aiCommentService.generateHealthGroupMemberEncouragement(userSeq, goalCriteria)
                .onSuccess { suggestions ->
                    _healthGroupSuggestions.value = suggestions
                    Log.d(tag, "[HEALTH_SUGGESTIONS] ✅ 성공: ${suggestions.size}개")
                    suggestions.forEachIndexed { index, text ->
                        Log.d(tag, "[HEALTH_SUGGESTIONS]   ${index + 1}. $text")
                    }
                }
                .onFailure { error ->
                    Log.e(tag, "[HEALTH_SUGGESTIONS] ❌ 실패: ${error.message}")
                    _healthGroupSuggestions.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    /**
     * ⑤ health_other_short: 빠른 넛지 메시지 로드 (케어 그룹)
     * 앱 세션 동안 캐싱: 첫 요청만 모델 호출, 이후 순차 출력
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

            // 캐시 없음 - 모델 호출
            _isLoading.value = true
            Log.d(tag, "[QUICK_NUDGE] 🔄 모델 호출 시작: userSeq=$userSeq")

            aiCommentService.generateQuickNudgeMessage(userSeq)
                .onSuccess { message ->
                    // 여러 문장으로 분리 (줄바꿈 기준)
                    val messages = message.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (messages.isNotEmpty()) {
                        // 첫 번째 문장 반환
                        val firstMessage = messages[0]
                        _quickNudgeMessage.value = firstMessage

                        // 캐시 저장 (다음 인덱스 계산)
                        val nextIndex = if (messages.size > 1) 1 else 0
                        nudgeCache[cacheKey] = messages to nextIndex

                        Log.d(tag, "[QUICK_NUDGE] ✅ 모델 응답 캐싱: 총 ${messages.size}개 문장")
                        Log.d(tag, "[QUICK_NUDGE] 📤 첫 문장 출력: $firstMessage")

                        // 서버에 알림 저장
                        sendPokeNotification(userSeq, groupSeq, firstMessage)
                    } else {
                        _quickNudgeMessage.value = ""
                        Log.w(tag, "[QUICK_NUDGE] ⚠️ 빈 응답")
                    }
                }
                .onFailure { error ->
                    Log.e(tag, "[QUICK_NUDGE] ❌ 실패: ${error.message}")
                    _quickNudgeMessage.value = ""
                }

            _isLoading.value = false
        }
    }

    /**
     * ⑥ wellness_other_short: 빠른 넛지 메시지 로드 (헬스 그룹)
     * 앱 세션 동안 캐싱: 첫 요청만 모델 호출, 이후 순차 출력
     * @param groupSeq 그룹 ID (같은 유저라도 다른 그룹이면 별도 캐시)
     */
    fun loadQuickWellnessNudgeMessage(groupSeq: Long, userSeq: Long, goalCriteria: GoalCriteria?) {
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

            // 캐시 없음 - 모델 호출
            _isLoading.value = true
            Log.d(tag, "[QUICK_WELLNESS_NUDGE] 🔄 모델 호출 시작: userSeq=$userSeq")

            aiCommentService.generateQuickWellnessNudgeMessage(userSeq, goalCriteria)
                .onSuccess { message ->
                    // 여러 문장으로 분리 (줄바꿈 기준)
                    val messages = message.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (messages.isNotEmpty()) {
                        // 첫 번째 문장 반환
                        val firstMessage = messages[0]
                        _quickNudgeMessage.value = firstMessage

                        // 캐시 저장 (다음 인덱스 계산)
                        val nextIndex = if (messages.size > 1) 1 else 0
                        nudgeCache[cacheKey] = messages to nextIndex

                        Log.d(tag, "[QUICK_WELLNESS_NUDGE] ✅ 모델 응답 캐싱: 총 ${messages.size}개 문장")
                        Log.d(tag, "[QUICK_WELLNESS_NUDGE] 📤 첫 문장 출력: $firstMessage")

                        // 서버에 알림 저장
                        sendPokeNotification(userSeq, groupSeq, firstMessage)
                    } else {
                        _quickNudgeMessage.value = ""
                        Log.w(tag, "[QUICK_WELLNESS_NUDGE] ⚠️ 빈 응답")
                    }
                }
                .onFailure { error ->
                    Log.e(tag, "[QUICK_WELLNESS_NUDGE] ❌ 실패: ${error.message}")
                    _quickNudgeMessage.value = ""
                }

            _isLoading.value = false
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
     * 상태 초기화
     */
    fun clearSuggestions() {
        _careGroupSuggestions.value = emptyList()
        _healthGroupSuggestions.value = emptyList()
        _quickNudgeMessage.value = ""
    }
}
