//package com.a307.linkcare.feature.ai.data.source
//
//import android.app.Application
////import android.llama.cpp.LLamaAndroid
//import android.util.Log
//import com.a307.linkcare.feature.ai.domain.model.ModelInfo
//import com.a307.linkcare.feature.ai.domain.model.ModelType
//import kotlinx.coroutines.flow.Flow
//import java.io.File
//import javax.inject.Inject
//import javax.inject.Singleton
//
///**
// * AI 모델 로컬 데이터 소스
// * LLamaAndroid를 래핑하여 데이터 계층 제공
// */
//@Singleton
//class AiModelLocalDataSource @Inject constructor(
//    private val application: Application
//) {
//    private val tag = "AiModelLocalDataSource"
//    private val llamaAndroid = LLamaAndroid.instance()
//
//    private var currentLoadedModel: ModelInfo? = null
//    private var isLoaded: Boolean = false
//
//    /**
//     * 사용 가능한 모델 목록
//     */
//    fun getAvailableModels(): List<ModelInfo> {
//        return listOf(
//            // 건강 관련 모델
//            ModelInfo("health_self.gguf", "건강 상담 (본인 관점)", ModelType.SELF),
//            ModelInfo("health_other.gguf", "건강 상담 (타인 관점)", ModelType.OTHER),
//            ModelInfo("health_other_short.gguf", "건강 상담 짧은 응답", ModelType.OTHER_SHORT),
//
//            // 웰니스 관련 모델
//            ModelInfo("wellness_self.gguf", "웰니스 상담 (본인 관점)", ModelType.SELF),
//            ModelInfo("wellness_other.gguf", "웰니스 상담 (타인 관점)", ModelType.OTHER),
//            ModelInfo("wellness_other_short.gguf", "웰니스 상담 짧은 응답", ModelType.OTHER_SHORT)
//        )
//    }
//
//    /**
//     * 모델 로드
//     */
//    suspend fun loadModel(modelInfo: ModelInfo): Result<Unit> {
//        return try {
//            // 1. 파일 경로 설정
//            val externalFilesDir = application.getExternalFilesDir(null)
//            val modelFile = File(externalFilesDir, modelInfo.filename)
//            val modelPath = modelFile.absolutePath
//
//            Log.d(tag, "모델 로딩 시작: ${modelInfo.displayName}")
//            Log.d(tag, "경로: $modelPath")
//
//            // 2. 파일 존재 확인
//            if (!modelFile.exists()) {
//                Log.e(tag, "모델 파일을 찾을 수 없습니다: $modelPath")
//                return Result.failure(Exception("모델 파일을 찾을 수 없습니다: ${modelInfo.filename}"))
//            }
//
//            val fileSizeMB = modelFile.length() / (1024 * 1024)
//            Log.d(tag, "파일 크기: ${fileSizeMB}MB")
//
//            // 3. 모델 로드
//            llamaAndroid.load(modelPath)
//
//            // 4. 상태 업데이트
//            isLoaded = true
//            currentLoadedModel = modelInfo
//            Log.d(tag, "모델 로드 완료: ${modelInfo.displayName}")
//
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e(tag, "모델 로드 실패", e)
//            isLoaded = false
//            currentLoadedModel = null
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * 모델 언로드
//     */
//    suspend fun unloadModel(): Result<Unit> {
//        return try {
//            if (isLoaded) {
//                Log.d(tag, "모델 언로드 중...")
//                llamaAndroid.unload()
//                isLoaded = false
//                currentLoadedModel = null
//                Log.d(tag, "모델 언로드 완료")
//            }
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e(tag, "모델 언로드 실패", e)
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * 메시지 전송 (Flow로 실시간 스트리밍)
//     */
//    fun sendMessage(message: String): Flow<String> {
//        Log.d(tag, "메시지 전송: $message")
//        return llamaAndroid.send(message, formatChat = false)
//    }
//
//    /**
//     * 현재 로드된 모델 정보
//     */
//    fun getCurrentModel(): ModelInfo? = currentLoadedModel
//
//    /**
//     * 모델 로드 상태
//     */
//    fun isModelLoaded(): Boolean = isLoaded
//}
