package com.a307.linkcare.feature.caregroup

import com.a307.linkcare.feature.watch.data.ExerciseSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExerciseSessionEntryPoint {
    fun exerciseSessionManager(): ExerciseSessionManager
}
