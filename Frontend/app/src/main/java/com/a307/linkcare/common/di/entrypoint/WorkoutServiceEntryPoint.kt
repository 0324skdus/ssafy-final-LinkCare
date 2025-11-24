package com.a307.linkcare.common.di.entrypoint

import com.a307.linkcare.feature.watch.manager.ExerciseSessionManager
import com.a307.linkcare.feature.workout.domain.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkoutServiceEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun exerciseSessionManager(): ExerciseSessionManager
}