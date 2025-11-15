package com.a307.linkcare.sdk.health.presentation

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.ArePermissionsGrantedUseCase
import com.a307.linkcare.feature.auth.RequestPermissionsUseCase
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.sdk.health.data.repository.HealthRepository
import com.a307.linkcare.sdk.health.domain.sync.*
import com.a307.linkcare.sdk.health.domain.sync.activitySummary.ActivitySummaryReader
import com.a307.linkcare.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.a307.linkcare.sdk.health.domain.sync.exercise.ExerciseReader
import com.a307.linkcare.sdk.health.domain.sync.heartRate.HeartRateReader
import com.a307.linkcare.sdk.health.domain.sync.sleep.SleepReader
import com.a307.linkcare.sdk.health.domain.sync.step.StepReader
import com.a307.linkcare.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import com.samsung.android.sdk.health.data.error.HealthDataException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HealthSyncViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val tokenStore: TokenStore,
    private val heartRateReader: HeartRateReader,
    private val sleepReader: SleepReader,
    private val bloodPressureReader: BloodPressureReader,
    private val waterIntakeReader: WaterIntakeReader,
    private val exerciseReader: ExerciseReader,
    private val stepReader: StepReader,
    private val activitySummaryReader: ActivitySummaryReader,
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val requestPermissionsUseCase: RequestPermissionsUseCase,


    ) : ViewModel() {

    private val TAG = "HealthSyncViewModel"

    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState


    /**
     * 오늘 하루 데이터 동기화
     */
    fun syncTodayData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {

                    val userId = tokenStore.getUserPk()?.toInt()
                if (userId == null) {
                    _syncState.value = SyncState.Error("사용자 정보를 찾을 수 없습니다")
                    return@launch
                }

                Log.d(TAG, "Starting sync for user: $userId")
                val today = LocalDate.now()
                val healthData = readDailyHealthData(today)

                // 읽은 데이터 로그
                Log.d(TAG, "=== 읽은 건강 데이터 ===")
                Log.d(TAG, "ActivitySummary: ${healthData.activitySummary}")
                Log.d(TAG, "HeartRate: ${healthData.heartRate?.size} items")
                Log.d(TAG, "Sleep: ${healthData.sleep?.size} items")
                Log.d(TAG, "WaterIntake: ${healthData.waterIntake}")
                Log.d(TAG, "BloodPressure: ${healthData.bloodPressure?.size} items")
                Log.d(TAG, "Exercise: ${healthData.exercise}")
                Log.d(TAG, "Step: ${healthData.step}")
                Log.d(TAG, "=====================")

                // 백엔드로 전송
                val result = repository.uploadUserDailyHealthData(userId, healthData)

                if (result.isSuccess) {
                    Log.d(TAG, "Sync successful")
                    _syncState.value = SyncState.Success("동기화 완료!")
                } else {
                    Log.e(TAG, "Sync failed: ${result.exceptionOrNull()?.message}")
                    _syncState.value = SyncState.Error("동기화 실패: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error", e)
                _syncState.value = SyncState.Error("동기화 중 오류 발생: ${e.message}")
            }
        }
    }

    /**
     * 삼성헬스에서 하루치 데이터 읽기
     */
    private suspend fun readDailyHealthData(date: LocalDate): DailyHealthData {
        Log.d(TAG, "Reading health data for date: $date")

        return DailyHealthData(
            activitySummary = try {
                activitySummaryReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading activity summary", e)
                null
            },
            heartRate = try {
                heartRateReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading heart rate", e)
                null
            },
            sleep = try {
                sleepReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading sleep", e)
                null
            },
            waterIntake = try {
                waterIntakeReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading water intake", e)
                null
            },
            bloodPressure = try {
                bloodPressureReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading blood pressure", e)
                null
            },
            exercise = try {
                exerciseReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading exercise", e)
                null
            },
            step = try {
                stepReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading steps", e)
                null
            }
        )
    }

    /**
     * 특정 날짜 범위의 데이터 동기화
     */
    fun syncDateRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {
                val userId = tokenStore.getUserPk()?.toInt()
                if (userId == null) {
                    _syncState.value = SyncState.Error("사용자 정보를 찾을 수 없습니다")
                    return@launch
                }

                Log.d(TAG, "Syncing range: $startDate to $endDate for user: $userId")

                var currentDate = startDate
                var successCount = 0
                var errorCount = 0

                while (!currentDate.isAfter(endDate)) {
                    try {
                        val healthData = readDailyHealthData(currentDate)
                        val result = repository.uploadUserDailyHealthData(userId, healthData)

                        if (result.isSuccess) {
                            successCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing date: $currentDate", e)
                        errorCount++
                    }

                    currentDate = currentDate.plusDays(1)
                }

                val message = "동기화 완료: 성공 $successCount, 실패 $errorCount"
                Log.d(TAG, message)
                _syncState.value = SyncState.Success(message)

            } catch (e: Exception) {
                Log.e(TAG, "Sync range error", e)
                _syncState.value = SyncState.Error("동기화 중 오류 발생: ${e.message}")
            }
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}
