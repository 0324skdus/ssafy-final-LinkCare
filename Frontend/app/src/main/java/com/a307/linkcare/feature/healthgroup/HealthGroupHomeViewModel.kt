package com.a307.linkcare.feature.healthgroup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.healthgroup.data.HealthGroupRepository
import com.a307.linkcare.feature.healthgroup.domain.WeeklyGroupGoalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthGroupHomeViewModel @Inject constructor(
    private val repository: HealthGroupRepository
) : ViewModel() {

    sealed class GoalState {
        object Idle : GoalState()
        object Loading : GoalState()
        data class Success(val goals: WeeklyGroupGoalResponse) : GoalState()
        data class Error(val msg: String) : GoalState()
    }

    private val _goalState = mutableStateOf<GoalState>(GoalState.Idle)
    val goalState: State<GoalState> = _goalState

    // 초기 로드: 현재 목표 조회 -> 없으면 AI 생성
    fun loadCurrentGoals(groupSeq: Long, requestDate: String) = viewModelScope.launch {
        android.util.Log.d("HealthGroupHomeViewModel", "🔍 초기 목표 로드")
        _goalState.value = GoalState.Loading

        // 먼저 현재 목표 조회
        val currentResult = repository.getCurrentGoals(groupSeq)

        currentResult.fold(
            onSuccess = { existingGoals ->
                if (existingGoals != null) {
                    // 목표가 이미 있음
                    android.util.Log.d("HealthGroupHomeViewModel", "✅ 기존 목표 있음")
                    _goalState.value = GoalState.Success(existingGoals)
                } else {
                    // 목표가 없음 -> AI로 생성
                    android.util.Log.d("HealthGroupHomeViewModel", "💡 목표 없음, AI로 생성")
                    generateGroupGoals(groupSeq, requestDate)
                }
            },
            onFailure = {
                android.util.Log.e("HealthGroupHomeViewModel", "❌ 목표 조회 실패: ${it.message}")
                _goalState.value = GoalState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun generateGroupGoals(groupSeq: Long, requestDate: String) = viewModelScope.launch {
        android.util.Log.d("HealthGroupHomeViewModel", "🎯 ViewModel: AI 목표 생성 시작")
        _goalState.value = GoalState.Loading

        val result = repository.generateGroupGoals(groupSeq, requestDate)

        _goalState.value = result.fold(
            onSuccess = {
                android.util.Log.d("HealthGroupHomeViewModel", "✅ ViewModel: 성공 상태로 변경")
                android.util.Log.d("HealthGroupHomeViewModel", "  - 목표 데이터: ${it.goalSteps} 걸음, ${it.goalKcal} kcal")
                GoalState.Success(it)
            },
            onFailure = {
                android.util.Log.e("HealthGroupHomeViewModel", "❌ ViewModel: 실패 상태로 변경 - ${it.message}")
                GoalState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun saveGoal(
        groupSeq: Long,
        metricType: String,
        goalValue: Long
    ) = viewModelScope.launch {
        android.util.Log.d("HealthGroupHomeViewModel", "💾 목표 저장 시작")
        _goalState.value = GoalState.Loading

        val result = repository.updateGoal(groupSeq, metricType, goalValue)

        _goalState.value = result.fold(
            onSuccess = {
                android.util.Log.d("HealthGroupHomeViewModel", "✅ 목표 저장 완료")
                GoalState.Success(it)
            },
            onFailure = {
                android.util.Log.e("HealthGroupHomeViewModel", "❌ 목표 저장 실패: ${it.message}")
                GoalState.Error(it.message ?: "Unknown error")
            }
        )
    }

    fun resetGoalState() {
        _goalState.value = GoalState.Idle
    }
}
