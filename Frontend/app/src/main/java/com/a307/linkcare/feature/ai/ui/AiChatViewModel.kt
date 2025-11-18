package com.a307.linkcare.feature.ai.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.ai.domain.model.AiMessage
import com.a307.linkcare.feature.ai.domain.model.HealthData
import com.a307.linkcare.feature.ai.domain.model.ModelConfig
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.model.ModelRegistry
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

    private val tag = "AiChatViewModel"

    // UI 상태
    var uiState by mutableStateOf(AiChatUiState())
        private set

    /**
     * 사용 가능한 모델 목록 (ModelRegistry에서 가져옴)
     */
    val availableModels: List<ModelInfo> = ModelRegistry.models.map { config ->
        ModelInfo.fromConfig(config)
    }

    init {
        Log.d(tag, "[INIT] AiChatViewModel 초기화")
        Log.d(tag, "[INIT] 사용 가능한 모델 수: ${availableModels.size}")
        // 기본 모델 선택
        uiState = uiState.copy(selectedModelIndex = 0)
        Log.d(tag, "[INIT] 기본 모델 선택: ${availableModels[0].displayName}")
    }

    /**
     * 모델 선택
     */
    fun selectModel(index: Int) {
        Log.d(tag, "[SELECT_MODEL] 모델 선택: ${availableModels[index].displayName}")
        uiState = uiState.copy(selectedModelIndex = index)
    }

    /**
     * 모델 로드
     */
    fun loadModel() {
        viewModelScope.launch {
            val modelInfo = availableModels[uiState.selectedModelIndex]
            Log.d(tag, "========================================")
            Log.d(tag, "[LOAD_MODEL] 모델 로드 시작: ${modelInfo.displayName}")
            uiState = uiState.copy(isLoading = true)

            val result = loadModelUseCase(modelInfo)
            result.fold(
                onSuccess = {
                    Log.d(tag, "[LOAD_MODEL] ✅ UseCase 성공")
                    uiState = uiState.copy(
                        isModelLoaded = true,
                        currentLoadedModel = modelInfo,
                        isLoading = false,
                        messages = uiState.messages + AiMessage(
                            content = "✅ 모델 로드 완료: ${modelInfo.displayName}",
                            isUser = false
                        )
                    )
                    Log.d(tag, "========================================")
                },
                onFailure = { error ->
                    Log.e(tag, "[LOAD_MODEL] ❌ UseCase 실패: ${error.message}")
                    uiState = uiState.copy(
                        isLoading = false,
                        messages = uiState.messages + AiMessage(
                            content = "❌ 모델 로드 실패: ${error.message}",
                            isUser = false
                        )
                    )
                    Log.e(tag, "========================================")
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
     * 메시지 전송 (HealthData 사용)
     */
    fun sendMessage(healthData: HealthData) {
        Log.d(tag, "========================================")
        Log.d(tag, "[SEND_MESSAGE] 메시지 전송 시작")
        Log.d(tag, "[SEND_MESSAGE] 데이터: $healthData")

        // 사용자 메시지 표시 (요약)
        val displayMessage = "걸음수: ${healthData.steps}보, 운동시간: ${healthData.duration}분"
        uiState = uiState.copy(
            messages = uiState.messages + AiMessage(content = displayMessage, isUser = true),
            isGenerating = true
        )

        viewModelScope.launch {
            sendMessageUseCase(healthData).collect { result ->
                when (result) {
                    is SendMessageResult.Loading -> {
                        Log.d(tag, "[SEND_MESSAGE] 상태: Loading")
                    }
                    is SendMessageResult.PromptGenerated -> {
                        Log.d(tag, "[SEND_MESSAGE] 📝 생성된 프롬프트:")
                        Log.d(tag, result.prompt)

                        // 현재 로드된 모델 정보 가져오기
                        val modelInfo = uiState.currentLoadedModel
                        val modelFileName = modelInfo?.filename ?: "알 수 없음"

                        // UI에 모델 파일명과 실제 입력 프롬프트 표시
                        uiState = uiState.copy(
                            messages = uiState.messages + AiMessage(
                                content = "🤖 사용 모델: ${modelInfo?.displayName ?: "알 수 없음"}\n📁 파일: $modelFileName\n\n📝 모델 입력:\n${result.prompt}",
                                isUser = true
                            )
                        )
                    }
                    is SendMessageResult.Streaming -> {
                        Log.d(tag, "[SEND_MESSAGE] 상태: Streaming, 길이: ${result.partialResponse.length}")
                        updateLastAiMessage("🤖 생성 중: ${result.partialResponse}")
                    }
                    is SendMessageResult.Success -> {
                        Log.d(tag, "[SEND_MESSAGE] ✅ 완료! 토큰 수: ${result.tokenCount}")
                        Log.d(tag, "[SEND_MESSAGE] 원본 출력: ${result.rawResponse}")
                        Log.d(tag, "[SEND_MESSAGE] 최종 응답: ${result.finalResponse}")

                        // UI에 원본 출력과 후처리 결과 표시
                        updateLastAiMessage(
                            "📤 원본 출력:\n${result.rawResponse}\n\n" +
                            "✨ 후처리 결과:\n${result.finalResponse}"
                        )
                        uiState = uiState.copy(isGenerating = false)
                        Log.d(tag, "========================================")
                    }
                    is SendMessageResult.Error -> {
                        Log.e(tag, "[SEND_MESSAGE] ❌ 에러: ${result.message}")
                        uiState = uiState.copy(
                            messages = uiState.messages + AiMessage(
                                content = "❌ ${result.message}",
                                isUser = false
                            ),
                            isGenerating = false
                        )
                        Log.e(tag, "========================================")
                    }
                }
            }
        }
    }

    /**
     * 메시지 전송 (userInput 기반 - UI 호환용)
     */
    fun sendMessage() {
        val input = uiState.userInput.trim()
        if (input.isEmpty()) {
            Log.w(tag, "[SEND_MESSAGE] 빈 메시지, 전송 취소")
            return
        }

        // userInput을 간단한 HealthData로 변환 (테스트용)
        val testData = HealthData(
            steps = 8000,
            duration = 45,
            distance = 6.0,
            kcal = 250,
            heartRate = 110
        )

        // userInput 초기화
        uiState = uiState.copy(userInput = "")

        // HealthData 기반 전송 호출
        sendMessage(testData)
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
     * 테스트 프롬프트 로드 (HealthData 생성 및 전송)
     */
    fun loadTestPrompt() {
        val modelInfo = availableModels[uiState.selectedModelIndex]

        Log.d(tag, "[TEST_PROMPT] 테스트 프롬프트 로드")
        Log.d(tag, "[TEST_PROMPT] 모델 카테고리: ${modelInfo.category}")
        Log.d(tag, "[TEST_PROMPT] 모델 관점: ${modelInfo.perspective}")

        // 카테고리에 따라 테스트 데이터 생성
        val testData = when (modelInfo.category) {
            com.a307.linkcare.feature.ai.domain.model.ModelCategory.HEALTH -> {
                // 건강(케어) 그룹 데이터
                HealthData(
                    steps = 8500,
                    duration = 45,
                    distance = 6.5,
                    kcal = 250,
                    heartRate = 110,
                    sleepHours = 7.5,
                    waterMl = 1500,
                    bloodPressure = "120/80"
                )
            }
            com.a307.linkcare.feature.ai.domain.model.ModelCategory.WELLNESS -> {
                // 웰니스(운동) 그룹 데이터
                HealthData(
                    steps = 12000,
                    duration = 60,
                    distance = 8.2,
                    kcal = 350,
                    heartRate = 125,
                    bestMetric = "운동시간",
                    bestValue = "90분"
                )
            }
        }

        Log.d(tag, "[TEST_PROMPT] 테스트 데이터: $testData")

        // 즉시 메시지 전송
        sendMessage(testData)
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
