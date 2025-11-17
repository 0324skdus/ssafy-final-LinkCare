//package com.a307.linkcare.feature.ai.data.repository
//
//import com.a307.linkcare.feature.ai.data.source.AiModelLocalDataSource
//import com.a307.linkcare.feature.ai.domain.model.ModelInfo
//import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
//import kotlinx.coroutines.flow.Flow
//import javax.inject.Inject
//import javax.inject.Singleton
//
///**
// * AI 모델 Repository 구현
// */
//@Singleton
//class AiModelRepositoryImpl @Inject constructor(
//    private val localDataSource: AiModelLocalDataSource
//) : AiModelRepository {
//
//    override suspend fun getAvailableModels(): List<ModelInfo> {
//        return localDataSource.getAvailableModels()
//    }
//
//    override suspend fun loadModel(modelInfo: ModelInfo): Result<Unit> {
//        return localDataSource.loadModel(modelInfo)
//    }
//
//    override suspend fun unloadModel(): Result<Unit> {
//        return localDataSource.unloadModel()
//    }
//
//    override fun sendMessage(message: String): Flow<String> {
//        return localDataSource.sendMessage(message)
//    }
//
//    override suspend fun getCurrentModel(): ModelInfo? {
//        return localDataSource.getCurrentModel()
//    }
//
//    override suspend fun isModelLoaded(): Boolean {
//        return localDataSource.isModelLoaded()
//    }
//}
