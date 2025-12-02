package com.a307.linkcare.feature.caregroup.ui.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.caregroup.data.model.response.HealthSummaryResponse
import com.a307.linkcare.feature.caregroup.data.model.response.SleepStatisticsResponse
import com.a307.linkcare.feature.caregroup.data.model.request.GroupStepUiState
import com.a307.linkcare.feature.caregroup.domain.repository.CareGroupRepository
import com.a307.linkcare.feature.caregroup.ui.detail.HealthToday
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CareGroupHomeViewModel @Inject constructor(
    private val repo: CareGroupRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow<Map<Int, HealthToday>>(emptyMap())
    val healthData: StateFlow<Map<Int, HealthToday>> = _healthData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadHealthData(userSeq: Int, date: LocalDate? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = if (date != null) {
                    repo.getDailyHealthDetailByDate(userSeq, date)
                } else {
                    repo.getDailyHealthDetail(userSeq)
                }
                if (data != null) {
                    _healthData.value = _healthData.value + (userSeq to data)
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHealthDataForMembers(userSeqs: List<Int>, date: LocalDate? = null) {
        userSeqs.forEach { userSeq ->
            loadHealthData(userSeq, date)
        }
    }

    var headerMessage by mutableStateOf<String?>(null)
        private set

    fun loadHeader(groupSeq: Long) {
        viewModelScope.launch {
            try {
                val res = repo.fetchWeeklyHeader(groupSeq)
                headerMessage = res.headerMessage
            } catch (e: Exception) {
                headerMessage = "함께 건강해져봐요!" // fallback
            }
        }
    }

    fun regenerateHeader(groupSeq: Long) {
        viewModelScope.launch {
            try {
                val res = repo.regenerateWeeklyHeader(groupSeq)
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

    fun loadGroupStepStatisticsByPeriod(groupSeq: Long, startDate: LocalDate, endDate: LocalDate) {
        _stepUiState.value = _stepUiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repo.getGroupStepStatisticsByPeriod(groupSeq, startDate, endDate)

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

    // 건강 피드백 저장
    private val _healthFeedbackMap = MutableStateFlow<Map<Int, HealthSummaryResponse>>(emptyMap())
    val healthFeedbackMap: StateFlow<Map<Int, HealthSummaryResponse>> = _healthFeedbackMap

    private val _isLoadingHealthFeedback = MutableStateFlow(false)
    val isLoadingHealthFeedback: StateFlow<Boolean> = _isLoadingHealthFeedback

    fun clearHealthFeedback() {
        _healthFeedbackMap.value = emptyMap()
    }

    fun loadHealthFeedbackForMembers(userSeqs: List<Int>, date: LocalDate) {
        Log.d("CareGroupHomeVM", "loadHealthFeedbackForMembers called with ${userSeqs.size} members, date=$date")
        _isLoadingHealthFeedback.value = true

        viewModelScope.launch {
            try {
                val targetDate = date
                userSeqs.forEach { userSeq ->
                    try {
                        Log.d("CareGroupHomeVM", "Loading health feedback for userSeq=$userSeq, date=$targetDate")
                        val feedback = repo.getHealthFeedback(userSeq, targetDate)
                        if (feedback != null) {
                            Log.d("CareGroupHomeVM", "Received health feedback for userSeq=$userSeq: ${feedback.status}")
                            _healthFeedbackMap.value = _healthFeedbackMap.value + (userSeq to feedback)
                        } else {
                            Log.d("CareGroupHomeVM", "No health feedback for userSeq=$userSeq")
                        }
                    } catch (e: Exception) {
                        Log.e("CareGroupHomeVM", "Error loading health feedback for userSeq=$userSeq", e)
                        e.printStackTrace()
                    }
                }
            } finally {
                _isLoadingHealthFeedback.value = false
            }
        }
    }

    // 그룹 멤버 한줄평 저장
    private val _memberComments = MutableStateFlow<Map<Long, String>>(emptyMap())
    val memberComments: StateFlow<Map<Long, String>> = _memberComments

    fun loadGroupMemberComments(groupSeq: Long) {
        viewModelScope.launch {
            try {
                val response = groupRepo.getGroupMemberComments(groupSeq)

                val comments = response.data?.comments ?: emptyList()
                comments.forEach { comment ->
                    Log.d("CareGroupHomeVM", "  - userSeq=${comment.userSeq}, userName=${comment.userName}, comment=${comment.comment}")
                }

                val commentMap = comments
                    .filter { !it.comment.isNullOrBlank() }
                    .associate { it.userSeq to it.comment!! }

                _memberComments.value = commentMap
            } catch (e: Exception) {
                Log.e("CareGroupHomeVM", "Error loading member comments: ${e.message}", e)
                _memberComments.value = emptyMap()
            }
        }
    }
}