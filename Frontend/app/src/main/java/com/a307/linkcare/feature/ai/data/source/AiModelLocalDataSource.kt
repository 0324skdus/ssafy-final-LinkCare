package com.a307.linkcare.feature.ai.data.source

import android.app.Application
import android.llama.cpp.LLamaAndroid
import android.util.Log
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.model.ModelPerspective
import com.a307.linkcare.feature.ai.domain.model.ModelType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모델 로드 작업
 */
private data class ModelLoadTask(
    val modelInfo: ModelInfo,
    val result: CompletableDeferred<Result<Unit>>
) : Comparable<ModelLoadTask> {
    override fun compareTo(other: ModelLoadTask): Int {
        // 우선순위가 낮은 숫자가 먼저 실행되도록
        return ModelPerspective.compare(this.modelInfo.perspective, other.modelInfo.perspective)
    }
}

/**
 * AI 모델 로컬 데이터 소스
 * LLamaAndroid를 래핑하여 데이터 계층 제공
 * 우선순위 큐 기반 모델 스케줄링 지원
 */
@Singleton
class AiModelLocalDataSource @Inject constructor(
    private val application: Application
) {
    private val tag = "AiModelLocalDataSource"
    private val llamaAndroid = LLamaAndroid.instance()

    private var currentLoadedModel: ModelInfo? = null
    private var isLoaded: Boolean = false
    private var isProcessing: Boolean = false

    // 동시성 제어를 위한 Mutex
    private val modelMutex = Mutex()

    // 우선순위 큐 (낮은 숫자가 높은 우선순위)
    private val taskQueue = PriorityQueue<ModelLoadTask>()
    private val queueMutex = Mutex()

    // 코루틴 스코프
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        Log.d(tag, "[INIT] AiModelLocalDataSource 초기화")
        Log.d(tag, "[INIT] 우선순위 큐 시스템 활성화")
    }

    /**
     * 사용 가능한 모델 목록
     */
    fun getAvailableModels(): List<ModelInfo> {
        Log.d(tag, "[GET_MODELS] 사용 가능한 모델 목록 조회")
        // ModelRegistry에서 모델 정보 가져오기
        val models = com.a307.linkcare.feature.ai.domain.model.ModelRegistry.models.map { config ->
            ModelInfo.fromConfig(config)
        }
        Log.d(tag, "[GET_MODELS] 모델 개수: ${models.size}")
        return models
    }

    /**
     * 모델 로드 (우선순위 큐 기반)
     */
    suspend fun loadModel(modelInfo: ModelInfo): Result<Unit> {
        // 같은 모델이 이미 로드되어 있으면 즉시 성공 반환
        if (isLoaded && currentLoadedModel?.filename == modelInfo.filename) {
            Log.d(tag, "[LOAD_MODEL] ✅ 동일한 모델이 이미 로드됨")
            Log.d(tag, "[LOAD_MODEL] 현재 모델: ${currentLoadedModel?.displayName}")
            return Result.success(Unit)
        }

        // 작업을 큐에 추가
        val deferred = CompletableDeferred<Result<Unit>>()
        val task = ModelLoadTask(modelInfo, deferred)

        queueMutex.withLock {
            taskQueue.offer(task)
            Log.d(tag, "[QUEUE] 작업 추가: ${modelInfo.displayName} (우선순위: ${modelInfo.perspective.priority})")
            Log.d(tag, "[QUEUE] 현재 큐 크기: ${taskQueue.size}")
        }

        // 큐 처리 시작
        processQueue()

        // 작업 완료 대기
        return deferred.await()
    }

    /**
     * 큐 처리 (순차 실행)
     */
    private fun processQueue() {
        scope.launch {
            // 이미 처리 중이면 중복 실행 방지
            val shouldProcess = queueMutex.withLock {
                if (isProcessing) {
                    Log.d(tag, "[QUEUE] 이미 처리 중, 스킵")
                    false
                } else {
                    isProcessing = true
                    true
                }
            }

            if (!shouldProcess) return@launch

            while (true) {
                val task = queueMutex.withLock {
                    taskQueue.poll()
                } ?: break

                Log.d(tag, "[QUEUE] 작업 실행: ${task.modelInfo.displayName}")

                val result = loadModelInternal(task.modelInfo)
                task.result.complete(result)

                Log.d(tag, "[QUEUE] 작업 완료: ${task.modelInfo.displayName}")
            }

            queueMutex.withLock {
                isProcessing = false
                Log.d(tag, "[QUEUE] 모든 작업 완료")
            }
        }
    }

    /**
     * 실제 모델 로드 수행 (내부용)
     */
    private suspend fun loadModelInternal(modelInfo: ModelInfo): Result<Unit> {
        return modelMutex.withLock {
            try {
                Log.d(tag, "========================================")
                Log.d(tag, "[LOAD_MODEL_START] 모델 로딩 시작")
                Log.d(tag, "[LOAD_MODEL_START] 모델명: ${modelInfo.displayName}")
                Log.d(tag, "[LOAD_MODEL_START] 파일명: ${modelInfo.filename}")
                Log.d(tag, "[LOAD_MODEL_START] 타입: ${modelInfo.type}")

                // 0. 같은 모델이 이미 로드되어 있으면 스킵
                if (isLoaded && currentLoadedModel?.filename == modelInfo.filename) {
                    Log.d(tag, "[LOAD_MODEL_SKIP] ✅ 동일한 모델이 이미 로드됨, 스킵")
                    Log.d(tag, "[LOAD_MODEL_SKIP] 현재 모델: ${currentLoadedModel?.displayName}")
                    Log.d(tag, "========================================")
                    return@withLock Result.success(Unit)
                }

                // 0-1. 다른 모델이 로드되어 있으면 먼저 언로드
                if (isLoaded && currentLoadedModel?.filename != modelInfo.filename) {
                    Log.d(tag, "[LOAD_MODEL_SWAP] ⚠️ 모델 교체 필요")
                    Log.d(tag, "[LOAD_MODEL_SWAP] 현재: ${currentLoadedModel?.displayName} (우선순위: ${currentLoadedModel?.perspective?.priority})")
                    Log.d(tag, "[LOAD_MODEL_SWAP] 요청: ${modelInfo.displayName} (우선순위: ${modelInfo.perspective.priority})")
                    Log.d(tag, "[LOAD_MODEL_SWAP] 기존 모델 언로드 중...")

                    llamaAndroid.unload()
                    isLoaded = false
                    currentLoadedModel = null

                    Log.d(tag, "[LOAD_MODEL_SWAP] ✅ 기존 모델 언로드 완료")
                }

                // 1. 파일 경로 설정
                val externalFilesDir = application.getExternalFilesDir(null)
                val modelFile = File(externalFilesDir, modelInfo.filename)
                val modelPath = modelFile.absolutePath

                Log.d(tag, "[LOAD_MODEL_PATH] External Files Dir: $externalFilesDir")
                Log.d(tag, "[LOAD_MODEL_PATH] 전체 경로: $modelPath")

                // 2. 파일 존재 확인
                if (!modelFile.exists()) {
                    Log.e(tag, "[LOAD_MODEL_ERROR] ❌ 모델 파일을 찾을 수 없습니다")
                    Log.e(tag, "[LOAD_MODEL_ERROR] 경로: $modelPath")
                    Log.e(tag, "[LOAD_MODEL_ERROR] 폴더 내 파일 목록:")
                    externalFilesDir?.listFiles()?.forEach {
                        Log.e(tag, "[LOAD_MODEL_ERROR]   - ${it.name}")
                    }
                    return@withLock Result.failure(Exception("모델 파일을 찾을 수 없습니다: ${modelInfo.filename}"))
                }

                val fileSizeMB = modelFile.length() / (1024 * 1024)
                Log.d(tag, "[LOAD_MODEL_INFO] ✅ 파일 존재 확인")
                Log.d(tag, "[LOAD_MODEL_INFO] 파일 크기: ${fileSizeMB}MB")
                Log.d(tag, "[LOAD_MODEL_INFO] 읽기 가능: ${modelFile.canRead()}")

                // 3. 모델 로드
                Log.d(tag, "[LOAD_MODEL_LLAMA] LlamaAndroid.load() 호출 중...")
                llamaAndroid.load(modelPath)
                Log.d(tag, "[LOAD_MODEL_LLAMA] ✅ LlamaAndroid.load() 완료")

                // 4. 상태 업데이트
                isLoaded = true
                currentLoadedModel = modelInfo

                Log.d(tag, "[LOAD_MODEL_SUCCESS] ✅ 모델 로드 완료!")
                Log.d(tag, "[LOAD_MODEL_SUCCESS] 현재 로드된 모델: ${modelInfo.displayName}")
                Log.d(tag, "========================================")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "========================================")
                Log.e(tag, "[LOAD_MODEL_EXCEPTION] ❌ 모델 로드 실패", e)
                Log.e(tag, "[LOAD_MODEL_EXCEPTION] Exception 타입: ${e.javaClass.simpleName}")
                Log.e(tag, "[LOAD_MODEL_EXCEPTION] 메시지: ${e.message}")
                Log.e(tag, "[LOAD_MODEL_EXCEPTION] Stack trace:", e)
                Log.e(tag, "========================================")

                isLoaded = false
                currentLoadedModel = null
                Result.failure(e)
            }
        }
    }

    /**
     * 모델 언로드
     */
    suspend fun unloadModel(): Result<Unit> {
        return modelMutex.withLock {
            try {
                if (isLoaded) {
                    Log.d(tag, "[UNLOAD_MODEL] 모델 언로드 시작...")
                    Log.d(tag, "[UNLOAD_MODEL] 현재 모델: ${currentLoadedModel?.displayName}")

                    llamaAndroid.unload()

                    isLoaded = false
                    currentLoadedModel = null

                    Log.d(tag, "[UNLOAD_MODEL] ✅ 모델 언로드 완료")
                } else {
                    Log.d(tag, "[UNLOAD_MODEL] 로드된 모델이 없습니다")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "[UNLOAD_MODEL_ERROR] ❌ 모델 언로드 실패", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 메시지 전송 (Flow로 실시간 스트리밍)
     */
    fun sendMessage(message: String): Flow<String> {
        Log.d(tag, "========================================")
        Log.d(tag, "[SEND_MESSAGE] 메시지 전송 시작")
        Log.d(tag, "[SEND_MESSAGE] 입력: $message")
        Log.d(tag, "[SEND_MESSAGE] 모델 로드 상태: $isLoaded")
        Log.d(tag, "[SEND_MESSAGE] 현재 모델: ${currentLoadedModel?.displayName}")
        Log.d(tag, "========================================")

        return llamaAndroid.send(message, formatChat = false)
    }

    /**
     * 현재 로드된 모델 정보
     */
    fun getCurrentModel(): ModelInfo? {
        Log.d(tag, "[GET_CURRENT_MODEL] 현재 모델: ${currentLoadedModel?.displayName ?: "없음"}")
        return currentLoadedModel
    }

    /**
     * 모델 로드 상태
     */
    fun isModelLoaded(): Boolean {
        Log.d(tag, "[IS_MODEL_LOADED] 로드 상태: $isLoaded")
        return isLoaded
    }
}
