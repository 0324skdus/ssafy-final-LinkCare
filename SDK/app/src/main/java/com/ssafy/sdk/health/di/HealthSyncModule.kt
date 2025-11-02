package com.ssafy.sdk.health.di

import android.content.Context
import android.content.SharedPreferences
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.ssafy.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.ssafy.sdk.health.domain.sync.exercise.ExerciseReader
import com.ssafy.sdk.health.domain.sync.heartRate.HeartRateReader
import com.ssafy.sdk.health.domain.sync.sleep.SleepReader
import com.ssafy.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthSyncModule {

    @Provides
    @Singleton
    fun provideHealthDataStore(
        @ApplicationContext context: Context
    ): HealthDataStore {
        return HealthDataService.getStore(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("health_sync_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideHeartRateReader(
        healthDataStore: HealthDataStore
    ): HeartRateReader {
        return HeartRateReader(healthDataStore)
    }

    @Provides
    @Singleton
    fun provideSleepReader(
        healthDataStore: HealthDataStore
    ): SleepReader {
        return SleepReader(healthDataStore)
    }

    @Provides
    @Singleton
    fun provideBloodPressureReader(
        healthDataStore: HealthDataStore
    ): BloodPressureReader {
        return BloodPressureReader(healthDataStore)
    }

    @Provides
    @Singleton
    fun provideWaterIntakeReader(
        healthDataStore: HealthDataStore
    ): WaterIntakeReader {
        return WaterIntakeReader(healthDataStore)
    }

    @Provides
    @Singleton
    fun provideExerciseReader(
        healthDataStore: HealthDataStore
    ): ExerciseReader {
        return ExerciseReader(healthDataStore)
    }
}