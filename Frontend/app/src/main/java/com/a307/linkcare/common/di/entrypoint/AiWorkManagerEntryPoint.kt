package com.a307.linkcare.common.di.entrypoint

import com.a307.linkcare.feature.ai.domain.worker.AiWorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiWorkManagerEntryPoint {
    fun aiWorkManager(): AiWorkManager
}