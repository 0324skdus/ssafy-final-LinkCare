package com.a307.linkcare.feature.caregroup.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.data.CareGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CareGroupHomeViewModel @Inject constructor(
    private val repo: CareGroupRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow<Map<Int, HealthToday>>(emptyMap())
    val healthData: StateFlow<Map<Int, HealthToday>> = _healthData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadHealthData(userSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("CareGroupHomeVM", "Loading health data for userSeq=$userSeq")
                val data = repo.getDailyHealthDetail(userSeq)
                if (data != null) {
                    Log.d("CareGroupHomeVM", "Received health data for userSeq=$userSeq")
                    _healthData.value = _healthData.value + (userSeq to data)
                } else {
                    Log.d("CareGroupHomeVM", "No health data for userSeq=$userSeq")
                }
            } catch (e: Exception) {
                Log.e("CareGroupHomeVM", "Error loading health data for userSeq=$userSeq", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHealthDataForMembers(userSeqs: List<Int>) {
        Log.d("CareGroupHomeVM", "loadHealthDataForMembers called with ${userSeqs.size} members: $userSeqs")
        userSeqs.forEach { userSeq ->
            loadHealthData(userSeq)
        }
    }

    var headerMessage by mutableStateOf<String?>(null)
        private set

    fun loadHeader(groupSeq: Long, today: LocalDate?) {
        viewModelScope.launch {
            try {
                val res = repo.fetchWeeklyHeader(groupSeq, today)
                headerMessage = res.headerMessage
            } catch (e: Exception) {
                headerMessage = "함께 건강해져봐요!" // fallback
            }
        }
    }

    // 수면 통계 저장 (StateFlow)
    private val _sleepStatistics = MutableStateFlow<SleepStatisticsResponse?>(null)
    val sleepStatistics: StateFlow<SleepStatisticsResponse?> = _sleepStatistics

    fun loadSleepStatistics(groupSeq: Long, start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            try {
                val res = repo.fetchWeeklySleepStatistics(groupSeq, start, end)
                _sleepStatistics.value = res
            } catch (e: Exception) {
                _sleepStatistics.value = null
            }
        }
    }

    private val _stepUiState = MutableStateFlow(GroupStepUiState())
    val stepUiState: StateFlow<GroupStepUiState> = _stepUiState

    fun loadGroupStepStatistics(groupSeq: Long) {
        _stepUiState.value = _stepUiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repo.getGroupStepStatistics(groupSeq)

            result
                .onSuccess { dto ->
                    val goal = dto.totalSteps
                    val progresses = dto.members.map { it.steps }

                    _stepUiState.value = GroupStepUiState(
                        isLoading = false,
                        goal = goal,
                        progresses = progresses,
                        members = dto.members
                    )
                }
                .onFailure { e ->
                    _stepUiState.value = _stepUiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류"
                    )
                }
        }
    }
}
