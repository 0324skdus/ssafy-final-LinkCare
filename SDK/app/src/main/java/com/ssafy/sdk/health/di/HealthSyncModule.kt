package com.ssafy.sdk.health.di

import android.content.Context
import android.content.SharedPreferences
import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.device.Device
import com.ssafy.sdk.health.data.repository.HealthRepository
import com.ssafy.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.ssafy.sdk.health.domain.sync.exercise.ExerciseReader
import com.ssafy.sdk.health.domain.sync.heartRate.HeartRateReader
import com.ssafy.sdk.health.domain.sync.sleep.SleepReader
import com.ssafy.sdk.health.domain.sync.step.StepReader
import com.ssafy.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import com.ssafy.sdk.health.domain.upload.SyncExerciseOnlyUseCase
import com.ssafy.sdk.health.domain.upload.UploadExerciseOnlyUseCase
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

    // DeviceManager 제공 추가
    @Provides
    @Singleton
    fun provideDeviceManager(
        healthDataStore: HealthDataStore
    ): DeviceManager {
        return healthDataStore.getDeviceManager()  // HealthDataStore에서 가져옴
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
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): HeartRateReader {
        return HeartRateReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideSleepReader(
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): SleepReader {
        return SleepReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideBloodPressureReader(
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): BloodPressureReader {
        return BloodPressureReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideWaterIntakeReader(
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): WaterIntakeReader {
        return WaterIntakeReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideExerciseReader(
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): ExerciseReader {
        return ExerciseReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideStepReader(
        healthDataStore: HealthDataStore,
        deviceManager: DeviceManager
    ): StepReader {
        return StepReader(healthDataStore, deviceManager)
    }

    @Provides
    @Singleton
    fun provideSyncExerciseOnlyUseCase(
        exerciseReader: ExerciseReader
    ): SyncExerciseOnlyUseCase {
        return SyncExerciseOnlyUseCase(exerciseReader)
    }

    @Provides
    @Singleton
    fun provideUploadExerciseOnlyUseCase(
        repository: HealthRepository
    ): UploadExerciseOnlyUseCase {
        return UploadExerciseOnlyUseCase(repository)
    }
}