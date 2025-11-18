//package com.a307.linkcare.feature.ai.domain.usecase
//
//import com.a307.linkcare.feature.ai.domain.model.ModelType
//import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import javax.inject.Inject
//
///**
// * AI 메시지 전송 UseCase
// */
//class SendMessageUseCase @Inject constructor(
//    private val repository: AiModelRepository
//) {
//    /**
//     * 메시지 전송 및 후처리된 응답 수신
//     * @param message 사용자 메시지
//     * @return 후처리된 최종 응답 Flow
//     */
//    operator fun invoke(message: String): Flow<SendMessageResult> = flow {
//        if (!repository.isModelLoaded()) {
//            emit(SendMessageResult.Error("모델이 로드되지 않았습니다"))
//            return@flow
//        }
//
//        val modelInfo = repository.getCurrentModel()
//        if (modelInfo == null) {
//            emit(SendMessageResult.Error("모델 정보를 찾을 수 없습니다"))
//            return@flow
//        }
//
//        emit(SendMessageResult.Loading)
//
//        var rawResponse = ""
//        var tokenCount = 0
//        val targetSentenceCount = getTargetSentenceCount(modelInfo.type)
//
//        try {
//            repository.sendMessage(message).collect { token ->
//                tokenCount++
//                rawResponse += token
//
//                // 실시간 토큰 전송 (UI에서 생성 중 표시용)
//                emit(SendMessageResult.Streaming(rawResponse))
//
//                // 조기 종료 체크 (10토큰마다 또는 중요 토큰 발견 시)
//                val shouldCheck = token.contains("</s>") ||
//                        token.contains("[SENT]") ||
//                        token.contains(".") || token.contains("!") || token.contains("?") ||
//                        tokenCount % 10 == 0
//
//                if (shouldCheck && shouldStopGeneration(rawResponse, modelInfo.type)) {
//                    // 목표 달성 - 후처리 후 종료
//                    val finalResponse = postprocessByModelType(rawResponse, modelInfo.type)
//                    emit(SendMessageResult.Success(finalResponse, tokenCount))
//                    return@collect
//                }
//            }
//
//            // Flow 정상 종료 (조기 종료 없이 끝난 경우)
//            val finalResponse = postprocessByModelType(rawResponse, modelInfo.type)
//            emit(SendMessageResult.Success(finalResponse, tokenCount))
//
//        } catch (e: Exception) {
//            emit(SendMessageResult.Error("응답 생성 실패: ${e.message}"))
//        }
//    }
//
//    /**
//     * 모델 타입별 목표 문장 수
//     */
//    private fun getTargetSentenceCount(modelType: ModelType): Int {
//        return when (modelType) {
//            ModelType.SELF -> 1
//            ModelType.OTHER -> 3
//            ModelType.OTHER_SHORT -> 10
//        }
//    }
//
//    /**
//     * 생성 중단 여부 확인
//     */
//    private fun shouldStopGeneration(rawText: String, modelType: ModelType): Boolean {
//        val targetCount = getTargetSentenceCount(modelType)
//        val sentTagCount = rawText.split("[SENT]").size - 1
//        val sentences = extractSentencesFromOutput(rawText)
//
//        // Priority 1: 비정상 패턴 감지
//        if (detectAbnormalPatterns(rawText)) {
//            return true
//        }
//
//        // Priority 2: 목표 문장 수 달성
//        when (modelType) {
//            ModelType.SELF -> {
//                val hasSelfTag = rawText.contains("[1SENT]")
//                val hasEndMarker = hasTextEndMarker(rawText)
//                if (hasSelfTag && hasEndMarker && sentences.size >= 1) {
//                    return true
//                }
//            }
//            ModelType.OTHER, ModelType.OTHER_SHORT -> {
//                val hasEndMarker = hasTextEndMarker(rawText)
//                if (sentTagCount >= targetCount && hasEndMarker) {
//                    return true
//                }
//            }
//        }
//
//        // Priority 3: </s> 토큰 (목표 달성 시)
//        if (rawText.contains("</s>") && sentences.size >= targetCount) {
//            return true
//        }
//
//        return false
//    }
//
//    /**
//     * 텍스트 종료 마커 감지
//     */
//    private fun hasTextEndMarker(text: String): Boolean {
//        val trimmed = text.trimEnd()
//        return trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?") ||
//                trimmed.endsWith("</b>") || trimmed.endsWith("</i>") ||
//                trimmed.endsWith("💪") || trimmed.endsWith("👍") || trimmed.endsWith("🏃")
//    }
//
//    /**
//     * 비정상 패턴 감지
//     */
//    private fun detectAbnormalPatterns(text: String): Boolean {
//        // 동일 이모티콘 5회 이상 반복
//        if (Regex("""([\p{So}\p{Sc}\p{Sk}\p{Sm}])\1{4,}""").containsMatchIn(text)) {
//            return true
//        }
//
//        // 텍스트 길이 초과 (1000자)
//        if (text.length > 1000) {
//            return true
//        }
//
//        // [SENT] 태그 과다 (20개 이상)
//        if (text.split("[SENT]").size - 1 > 20) {
//            return true
//        }
//
//        return false
//    }
//
//    /**
//     * 문장 추출
//     */
//    private fun extractSentencesFromOutput(text: String): List<String> {
//        var processed = text
//        processed = processed.replace(Regex("""\[\d+SENT\]\s*"""), "")
//
//        if (processed.contains("</s>")) {
//            processed = processed.split("</s>")[0]
//        }
//
//        val sentences = if (processed.contains("[SENT]")) {
//            processed.split("[SENT]").filter { it.isNotEmpty() }
//        } else {
//            processed.split(Regex("""[.!?]\s+""")).filter { it.isNotEmpty() }
//        }
//
//        return sentences.map { it.trim() }
//    }
//
//    /**
//     * 문장 정리
//     */
//    private fun cleanSentence(sentence: String): String {
//        var cleaned = sentence
//        cleaned = cleaned.replace(Regex("""<[^>]+>"""), "")
//        cleaned = cleaned.replace(Regex("""[▶►◀◄]"""), "")
//        cleaned = cleaned.replace(Regex("""([♠♥♣♦☎️📞])\1{2,}"""), "")
//        cleaned = cleaned.replace(Regex("""([!?]){2,}"""), "$1")
//        cleaned = cleaned.replace(Regex("""\[[^\]]{3,20}\]"""), "")
//        cleaned = cleaned.replace(Regex("""\s+"""), " ")
//        cleaned = cleaned.trim()
//
//        if (cleaned.isNotEmpty() &&
//            !cleaned.endsWith(".") &&
//            !cleaned.endsWith("!") &&
//            !cleaned.endsWith("?") &&
//            !cleaned.endsWith("요") &&
//            !cleaned.endsWith("다") &&
//            !cleaned.endsWith("네요") &&
//            !cleaned.endsWith("어요") &&
//            !cleaned.endsWith("습니다")
//        ) {
//            cleaned += "."
//        }
//
//        return cleaned
//    }
//
//    /**
//     * 모델 타입별 후처리
//     */
//    private fun postprocessByModelType(text: String, modelType: ModelType): String {
//        val sentences = extractSentencesFromOutput(text)
//        if (sentences.isEmpty()) return "[출력 없음]"
//
//        return when (modelType) {
//            ModelType.SELF -> {
//                // 1문장
//                cleanSentence(sentences[0])
//            }
//            ModelType.OTHER -> {
//                // 3문장, 공백으로 구분
//                sentences.take(3).map { cleanSentence(it) }.joinToString(" ")
//            }
//            ModelType.OTHER_SHORT -> {
//                // 10문장, 줄바꿈으로 구분
//                sentences.take(10).map { cleanSentence(it) }.joinToString("\n")
//            }
//        }
//    }
//}
//
///**
// * 메시지 전송 결과
// */
//sealed class SendMessageResult {
//    object Loading : SendMessageResult()
//    data class Streaming(val partialResponse: String) : SendMessageResult()
//    data class Success(val finalResponse: String, val tokenCount: Int) : SendMessageResult()
//    data class Error(val message: String) : SendMessageResult()
//}
