package com.a307.linkcare.feature.watch.service

import com.a307.linkcare.feature.watch.data.ExerciseSessionManager
import com.a307.linkcare.feature.workout.data.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkoutServiceEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun exerciseSessionManager(): ExerciseSessionManager
}
