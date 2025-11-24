package com.a307.linkcare.common.di.module

import com.a307.linkcare.feature.ai.data.repository.AiModelRepositoryImpl
import com.a307.linkcare.feature.ai.domain.repository.AiModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiModelRepository(
        aiModelRepositoryImpl: AiModelRepositoryImpl
    ): AiModelRepository
}