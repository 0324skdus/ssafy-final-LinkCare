/*
 * Copyright 2025 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ssafy.sdk.health.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.ssafy.sdk.health.data.model.ActivityData
import com.ssafy.sdk.health.data.model.BloodPressureData
import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.data.model.HealthError
import com.ssafy.sdk.health.data.model.HeartRatesData
import com.ssafy.sdk.health.data.model.SleepData
import com.ssafy.sdk.health.data.model.SleepGoalData
import com.ssafy.sdk.health.data.model.StepData
import com.ssafy.sdk.health.data.model.WaterIntakeData
import com.ssafy.sdk.health.domain.ArePermissionsGrantedUseCase
import com.ssafy.sdk.health.domain.activity.ReadTotalSummaryUserCase
import com.ssafy.sdk.health.domain.bloodPressures.ReadBloodPressureUseCase
import com.ssafy.sdk.health.domain.exercise.ReadExerciseUseCase
import com.ssafy.sdk.health.domain.heartRates.ReadHeartRateUserCase
import com.ssafy.sdk.health.domain.sleeps.ReadTotalSleepsUseCase
import com.ssafy.sdk.health.domain.steps.ReadStepDataUseCase
import com.ssafy.sdk.health.domain.waterIntake.ReadGroupedWaterIntakeUseCase
import com.ssafy.sdk.health.domain.waterIntake.ReadWaterIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val readStepDataUseCase: ReadStepDataUseCase,
    private val readTotalSummaryUserCase: ReadTotalSummaryUserCase,
    private val readHeartRateUserCase: ReadHeartRateUserCase,
    private val readWaterIntakeUseCase: ReadWaterIntakeUseCase,
    private val readTotalSleepsUseCase: ReadTotalSleepsUseCase,
    private val readExerciseUseCase: ReadExerciseUseCase,
    private val readBloodPressureUseCase: ReadBloodPressureUseCase,

    ) : ViewModel() {
    init {
        Log.i(TAG, "init()")
        checkPermissions()
    }

    private val _state =
        MutableStateFlow(
            State(
                permissionRequested = false,
                permissionsGranted = false,
                steps = StepData(0, 0, ArrayList()),
                activities = ActivityData(null, 0.toDouble(), 0.toLong(), 0.toDouble(), 0.toDouble()),
                heartRates = listOf(
                    HeartRatesData(
                        "",
                        null,
                        null,
                        ZoneOffset.UTC,
                    null,
                        0f,
                        emptyList(),
                        0f,
                        0f)
                ),
                waterIntake = WaterIntakeData(
                    null,
                    emptyList(),
                    0f,
                    0f
                ),
                sleeps = SleepData(
                    null,
                    emptyList(),
                    0L,
                    SleepGoalData(
                        LocalTime.MIDNIGHT,
                        LocalTime.MIDNIGHT
                    )
                ),
                exercises = ExerciseData(
                    null,
                    emptyList(),
                    0L,
                    0f
                ),
                bloodPressures = BloodPressureData(
                    null,
                    emptyList()
                ),
                refresh = false,
                errorLevel = null,
            )
        )
    val state: StateFlow<State> = _state

    fun refresh() {
        Log.i(TAG, "refresh()")
        _state.update { currentState ->
            currentState.copy(
                refresh = true
            )
        }
    }

    private fun checkPermissions() {
        Log.i(TAG, "checkPermissions()")
        viewModelScope.launch {
            try {
                val permissionsGranted = arePermissionsGrantedUseCase()
                if (permissionsGranted) {
                    refresh()
                } else {
                    Log.i(TAG, "Permission not granted so far")
                }
                _state.update { currentState ->
                    currentState.copy(
                        permissionRequested = true,
                        permissionsGranted = permissionsGranted
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            }
        }
    }


    fun readSteps() {
        Log.i(TAG, "readSteps()")
        viewModelScope.launch {
            try {
                val steps = readStepDataUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        steps = steps,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readActivities() {
        Log.i(TAG, "readActivities()")
        viewModelScope.launch {
            try {
                val activities = readTotalSummaryUserCase()
                _state.update { currentState ->
                    currentState.copy(
                        activities = activities,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readHeartRates() {
        Log.i(TAG, "readHeartRates()")
        viewModelScope.launch {
            try {
                val heartRates = readHeartRateUserCase()
                _state.update { currentState ->
                    currentState.copy(
                        heartRates = heartRates,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readWaterIntake() {
        Log.i(TAG, "readWaterIntake()")
        viewModelScope.launch {
            try {
                val waterIntake = readWaterIntakeUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        waterIntake = waterIntake,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readSleep() {
        Log.i(TAG, "readSleep()")
        viewModelScope.launch {
            try {
                val sleeps = readTotalSleepsUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        sleeps = sleeps,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readExercises() {
        Log.i(TAG, "readExercises()")
        viewModelScope.launch {
            try {
                val exercises = readExerciseUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        exercises = exercises,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun readBloodPressures() {
        Log.i(TAG, "readBloodPressures()")
        viewModelScope.launch {
            try {
                val bloodPressures = readBloodPressureUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        bloodPressures = bloodPressures,
                        errorLevel = null
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun userAcceptedPermissions(agreed: Boolean) {
        Log.i(TAG, "userAcceptedPermissions")
        _state.update { currentState ->
            currentState.copy(
                permissionRequested = false,
                permissionsGranted = agreed
            )
        }
        refresh()
    }

    fun handleHealthDataException(healthDataException: HealthDataException) {
        val errorMessage = healthDataException.errorMessage
        val errorCode = healthDataException.errorCode ?: 0
        val healthError =
            HealthError(healthDataException, errorCode.toString(), errorMessage, false)
        if (healthDataException is ResolvablePlatformException && healthDataException.hasResolution) {
            Log.i(
                TAG,
                "Resolvable Exception; message: ${healthDataException.errorMessage}"
            )
            healthError.error = healthDataException
            healthError.resolvable = true
        } else if (healthDataException is AuthorizationException) {
            Log.i(TAG, "Authorization Exception")
        } else if (healthDataException is InvalidRequestException) {
            Log.i(TAG, "Invalid Request Exception")
        } else if (healthDataException is PlatformInternalException) {
            Log.i(TAG, "Platform Internal Exception")
        }
        _state.update { currentState ->
            currentState.copy(errorLevel = healthError)
        }
    }
}

data class State(
    val permissionRequested: Boolean,
    val permissionsGranted: Boolean,
    val steps: StepData,
    val activities: ActivityData,
    val heartRates: List<HeartRatesData>,
    val waterIntake: WaterIntakeData,
    val sleeps: SleepData,
    val exercises: ExerciseData,
    val bloodPressures: BloodPressureData,
    val refresh: Boolean,
    val errorLevel: HealthError?,
)