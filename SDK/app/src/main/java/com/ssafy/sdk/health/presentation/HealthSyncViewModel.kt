package com.ssafy.sdk.health.presentation

import android.app.Activity
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.domain.upload.UploadAllHealthDataUseCase
import com.ssafy.sdk.health.domain.ArePermissionsGrantedUseCase
import com.ssafy.sdk.health.domain.RequestPermissionsUseCase
import com.ssafy.sdk.health.domain.sync.SyncAllHealthDataUseCase
import com.ssafy.sdk.health.domain.sync.SyncDailyHealthDataUseCase
import com.ssafy.sdk.health.domain.upload.SyncExerciseOnlyUseCase
import com.ssafy.sdk.health.domain.upload.UploadDailyHealthDataUseCase
import com.ssafy.sdk.health.domain.upload.UploadExerciseOnlyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 동기화 진행 상태
 */
sealed class SyncProgress {
    object Idle : SyncProgress()
    object PermissionRequired : SyncProgress()
    object CollectingData : SyncProgress()
    data class FullSyncInProgress(
        val dataType: String,
        val current: Int,
        val total: Int,
        val percentage: Int
    ) : SyncProgress()
    data class Uploading(val message: String) : SyncProgress()
    data class Success(val message: String) : SyncProgress()
    data class Error(val message: String) : SyncProgress()
}

@HiltViewModel
class HealthSyncViewModel @Inject constructor(
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val syncDailyHealthDataUseCase: SyncDailyHealthDataUseCase,
    private val syncAllHealthDataUseCase: SyncAllHealthDataUseCase,
    private val uploadDailyHealthDataUseCase: UploadDailyHealthDataUseCase,
    private val uploadAllHealthDataUseCase: UploadAllHealthDataUseCase,
    private val sharedPreferences: SharedPreferences,
    private val requestPermissionsUseCase: RequestPermissionsUseCase,
    private val syncExerciseOnlyUseCase: SyncExerciseOnlyUseCase,
    private val uploadExerciseOnlyUseCase: UploadExerciseOnlyUseCase,
) : ViewModel() {

    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    /**
     * 권한 요청
     */
    fun requestPermissions(activity: Activity) {
        viewModelScope.launch {
            try {
                val granted = requestPermissionsUseCase(activity)
                _permissionsGranted.value = granted

                if (!granted) {
                    _syncProgress.value = SyncProgress.PermissionRequired
                }
            } catch (e: HealthDataException) {
                _syncProgress.value = SyncProgress.Error("권한 요청 실패: ${e.message}")
            }
        }
    }

    /**
     * 앱 시작 시 권한 체크
     */
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                _permissionsGranted.value = arePermissionsGrantedUseCase()
            } catch (e: HealthDataException) {
                _permissionsGranted.value = false
            }
        }
    }

    companion object {
        private const val PREF_IS_FIRST_SYNC = "is_first_sync"
        private const val PREF_LAST_SYNC_TIME = "last_sync_time"
    }

    /**
     * 건강 데이터 동기화 & 업로드
     * 1. 권한 체크
     * 2. 데이터 수집 (권한 기반, userId 불필요)
     * 3. 서버 전송 (userId 필요)
     */
    fun syncHealthData(userId: Int) {
        viewModelScope.launch {
            try {
                // 1단계: 권한 확인
                if (!arePermissionsGrantedUseCase()) {
                    _syncProgress.value = SyncProgress.PermissionRequired
                    return@launch
                }

                val isFirstSync = sharedPreferences.getBoolean(PREF_IS_FIRST_SYNC, true)

                if (isFirstSync) {
                    performFullSyncAndUpload(userId)
                } else {
                    performDailySyncAndUpload(userId)
                }
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress.Error(e.message ?: "동기화 실패")
            }
        }
    }

    /**
     * 전체 동기화 강제 실행
     */
    fun forceFullSync(userId: Int) {
        viewModelScope.launch {
            try {
                if (!arePermissionsGrantedUseCase()) {
                    _syncProgress.value = SyncProgress.PermissionRequired
                    return@launch
                }

                performFullSyncAndUpload(userId)
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress.Error(e.message ?: "전체 동기화 실패")
            }
        }
    }

    /**
     * 오늘 데이터만 동기화 & 업로드
     */
    private suspend fun performDailySyncAndUpload(userId: Int) {
        _syncProgress.value = SyncProgress.CollectingData

        // 1. 데이터 수집 (권한 기반)
        val dailyData = syncDailyHealthDataUseCase()

        // 2. 서버 전송 (userId 사용)
        _syncProgress.value = SyncProgress.Uploading("서버에 전송 중...")

        val result = uploadDailyHealthDataUseCase(userId, dailyData)

        result.fold(
            onSuccess = {
                sharedPreferences.edit()
                    .putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                    .apply()

                _syncProgress.value = SyncProgress.Success("오늘 데이터 동기화 & 업로드 완료!")
            },
            onFailure = { error ->
                _syncProgress.value = SyncProgress.Error(error.message ?: "업로드 실패")
            }
        )
    }

    /**
     * 전체 데이터 동기화 & 업로드
     */
    private suspend fun performFullSyncAndUpload(userId: Int) {
        // 1. 전체 데이터 수집 (권한 기반)
        val allData = syncAllHealthDataUseCase(
            onProgress = { dataType, current, total ->
                val percentage = if (total > 0) {
                    (current.toFloat() / total * 100).toInt()
                } else 0

                _syncProgress.value = SyncProgress.FullSyncInProgress(
                    dataType = dataType,
                    current = current,
                    total = total,
                    percentage = percentage
                )
            },
            useParallel = true,
            yearsPerChunk = 1L
        )

        // 2. 서버 전송 (userId 사용)
        _syncProgress.value = SyncProgress.Uploading("서버에 전송 중...")

        val result = uploadAllHealthDataUseCase(userId, allData)

        result.fold(
            onSuccess = {
                sharedPreferences.edit()
                    .putBoolean(PREF_IS_FIRST_SYNC, false)
                    .putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                    .apply()

                _syncProgress.value = SyncProgress.Success("전체 동기화 & 업로드 완료!")
            },
            onFailure = { error ->
                _syncProgress.value = SyncProgress.Error(error.message ?: "업로드 실패")
            }
        )
    }

    /**
     * 운동 데이터만 동기화 & 업로드 (워치 병합용)
     */
    fun syncExerciseOnly(userId: Int) {
        viewModelScope.launch {
            try {
                // 1단계: 권한 확인
                if (!arePermissionsGrantedUseCase()) {
                    _syncProgress.value = SyncProgress.PermissionRequired
                    return@launch
                }

                _syncProgress.value = SyncProgress.CollectingData

                // 2단계: 운동 데이터만 수집
                val exercises = syncExerciseOnlyUseCase()

                // 3단계: 서버 전송
                _syncProgress.value = SyncProgress.Uploading("운동 데이터 전송 중...")

                val result = uploadExerciseOnlyUseCase(userId, exercises)

                result.fold(
                    onSuccess = {
                        _syncProgress.value = SyncProgress.Success("운동 데이터 동기화 완료!")
                    },
                    onFailure = { error ->
                        _syncProgress.value = SyncProgress.Error(error.message ?: "업로드 실패")
                    }
                )
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress.Error(e.message ?: "동기화 실패")
            }
        }
    }

    /**
     * 동기화 상태 초기화
     */
    fun resetSyncStatus() {
        _syncProgress.value = SyncProgress.Idle
    }

    /**
     * 최초 동기화 여부 확인
     */
    fun isFirstSync(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_FIRST_SYNC, true)
    }

    /**
     * 마지막 동기화 시간 조회
     */
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(PREF_LAST_SYNC_TIME, 0L)
    }
}
