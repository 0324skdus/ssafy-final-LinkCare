package com.a307.linkcare.feature.ai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.ai.domain.model.AiMessage
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.usecase.LoadModelUseCase
import com.a307.linkcare.feature.ai.domain.usecase.SendMessageResult
import com.a307.linkcare.feature.ai.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 채팅 ViewModel
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val loadModelUseCase: LoadModelUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    // UI 상태
    var uiState by mutableStateOf(AiChatUiState())
        private set

    /**
     * 사용 가능한 모델 목록 (하드코딩 - 실제로는 Repository에서 가져옴)
     */
    val availableModels = listOf(
        ModelInfo("health_self.gguf", "건강 상담 (본인 관점)", com.a307.linkcare.feature.ai.domain.model.ModelType.SELF),
        ModelInfo("health_other.gguf", "건강 상담 (타인 관점)", com.a307.linkcare.feature.ai.domain.model.ModelType.OTHER),
        ModelInfo("health_other_short.gguf", "건강 상담 짧은 응답", com.a307.linkcare.feature.ai.domain.model.ModelType.OTHER_SHORT),
        ModelInfo("wellness_self.gguf", "웰니스 상담 (본인 관점)", com.a307.linkcare.feature.ai.domain.model.ModelType.SELF),
        ModelInfo("wellness_other.gguf", "웰니스 상담 (타인 관점)", com.a307.linkcare.feature.ai.domain.model.ModelType.OTHER),
        ModelInfo("wellness_other_short.gguf", "웰니스 상담 짧은 응답", com.a307.linkcare.feature.ai.domain.model.ModelType.OTHER_SHORT)
    )

    init {
        // 기본 모델 선택
        uiState = uiState.copy(selectedModelIndex = 0)
    }

    /**
     * 모델 선택
     */
    fun selectModel(index: Int) {
        uiState = uiState.copy(selectedModelIndex = index)
    }

    /**
     * 모델 로드
     */
    fun loadModel() {
        viewModelScope.launch {
            val modelInfo = availableModels[uiState.selectedModelIndex]
            uiState = uiState.copy(isLoading = true)

            val result = loadModelUseCase(modelInfo)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isModelLoaded = true,
                        currentLoadedModel = modelInfo,
                        isLoading = false,
                        messages = uiState.messages + AiMessage(
                            content = "✅ 모델 로드 완료: ${modelInfo.displayName}",
                            isUser = false
                        )
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        messages = uiState.messages + AiMessage(
                            content = "❌ 모델 로드 실패: ${error.message}",
                            isUser = false
                        )
                    )
                }
            )
        }
    }

    /**
     * 사용자 입력 업데이트
     */
    fun updateUserInput(text: String) {
        uiState = uiState.copy(userInput = text)
    }

    /**
     * 메시지 전송
     */
    fun sendMessage() {
        val message = uiState.userInput.trim()
        if (message.isEmpty()) return

        // 사용자 메시지 추가
        uiState = uiState.copy(
            messages = uiState.messages + AiMessage(content = message, isUser = true),
            userInput = "",
            isGenerating = true
        )

        viewModelScope.launch {
            sendMessageUseCase(message).collect { result ->
                when (result) {
                    is SendMessageResult.Loading -> {
                        // 로딩 표시 (이미 isGenerating = true)
                    }
                    is SendMessageResult.Streaming -> {
                        // 실시간 스트리밍 (생성 중 표시)
                        updateLastAiMessage("🤖 생성 중: ${result.partialResponse}")
                    }
                    is SendMessageResult.Success -> {
                        // 최종 응답
                        updateLastAiMessage(result.finalResponse)
                        uiState = uiState.copy(isGenerating = false)
                    }
                    is SendMessageResult.Error -> {
                        // 에러
                        uiState = uiState.copy(
                            messages = uiState.messages + AiMessage(
                                content = "❌ ${result.message}",
                                isUser = false
                            ),
                            isGenerating = false
                        )
                    }
                }
            }
        }
    }

    /**
     * 마지막 AI 메시지 업데이트 (실시간 스트리밍용)
     */
    private fun updateLastAiMessage(content: String) {
        val messages = uiState.messages.toMutableList()
        if (messages.isEmpty() || messages.last().isUser) {
            // 새 AI 메시지 추가
            messages.add(AiMessage(content = content, isUser = false))
        } else {
            // 마지막 AI 메시지 업데이트
            messages[messages.lastIndex] = AiMessage(content = content, isUser = false)
        }
        uiState = uiState.copy(messages = messages)
    }

    /**
     * 메시지 초기화
     */
    fun clearMessages() {
        uiState = uiState.copy(messages = emptyList())
    }

    /**
     * 테스트 프롬프트 로드
     */
    fun loadTestPrompt() {
        val testPrompt = """
            오늘 걸음수 8500보, 운동시간 60분, 운동거리 6.5km,
            소모 칼로리 250kcal, 평균 심박수 110bpm,
            수면시간 7.5시간, 음수량 1500ml, 혈압 120/80.
            이 데이터를 바탕으로 오늘의 건강 상태를 요약해주세요.
        """.trimIndent()

        uiState = uiState.copy(userInput = testPrompt)
    }
}

/**
 * AI 채팅 UI 상태
 */
data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val userInput: String = "",
    val isModelLoaded: Boolean = false,
    val isGenerating: Boolean = false,
    val isLoading: Boolean = false,
    val selectedModelIndex: Int = 0,
    val currentLoadedModel: ModelInfo? = null
)
