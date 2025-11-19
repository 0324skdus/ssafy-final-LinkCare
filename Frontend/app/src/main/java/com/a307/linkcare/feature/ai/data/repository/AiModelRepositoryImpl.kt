package com.a307.linkcare.feature.ai.data.repository

import android.util.Log
import com.a307.linkcare.feature.ai.data.source.AiModelLocalDataSource
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 모델 Repository 구현
 */
@Singleton
class AiModelRepositoryImpl @Inject constructor(
    private val localDataSource: AiModelLocalDataSource
) : AiModelRepository {

    private val tag = "AiModelRepository"

    init {
        Log.d(tag, "[INIT] AiModelRepositoryImpl 초기화")
    }

    override suspend fun getAvailableModels(): List<ModelInfo> {
        Log.d(tag, "[REPO] getAvailableModels() 호출")
        return localDataSource.getAvailableModels()
    }

    override suspend fun loadModel(modelInfo: ModelInfo): Result<Unit> {
        Log.d(tag, "[REPO] loadModel() 호출: ${modelInfo.displayName}")
        return localDataSource.loadModel(modelInfo)
    }

    override suspend fun unloadModel(): Result<Unit> {
        Log.d(tag, "[REPO] unloadModel() 호출")
        return localDataSource.unloadModel()
    }

    override fun sendMessage(message: String): Flow<String> {
        Log.d(tag, "[REPO] sendMessage() 호출")
        return localDataSource.sendMessage(message)
    }

    override suspend fun getCurrentModel(): ModelInfo? {
        Log.d(tag, "[REPO] getCurrentModel() 호출")
        return localDataSource.getCurrentModel()
    }

    override suspend fun isModelLoaded(): Boolean {
        Log.d(tag, "[REPO] isModelLoaded() 호출")
        return localDataSource.isModelLoaded()
    }
}
