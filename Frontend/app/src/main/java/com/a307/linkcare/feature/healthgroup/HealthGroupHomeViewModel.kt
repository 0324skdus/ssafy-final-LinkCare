package com.a307.linkcare.feature.healthgroup

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.data.GroupRepository
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.healthgroup.data.HealthGroupRepository
import com.a307.linkcare.feature.healthgroup.domain.ActualActivity
import com.a307.linkcare.feature.healthgroup.domain.MemberWithActivity
import com.a307.linkcare.feature.healthgroup.domain.WeeklyGroupGoalResponse
import com.a307.linkcare.feature.caregroup.data.endOfWeek
import com.a307.linkcare.feature.caregroup.data.startOfWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HealthGroupHomeViewModel @Inject constructor(
    private val repository: HealthGroupRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

    sealed class GoalState {
        object Idle : GoalState()
        object Loading : GoalState()
        data class Success(val goals: WeeklyGroupGoalResponse) : GoalState()
        data class Error(val msg: String) : GoalState()
    }

    private val _goalState = mutableStateOf<GoalState>(GoalState.Idle)
    val goalState: State<GoalState> = _goalState

    // --- 활동 데이터 관련 상태 ---
    private val _totalStats = MutableStateFlow<ActualActivity?>(null)
    val totalStats: StateFlow<ActualActivity?> = _totalStats

    private val _membersWithActivity = MutableStateFlow<List<MemberWithActivity>>(emptyList())
    val membersWithActivity: StateFlow<List<MemberWithActivity>> = _membersWithActivity

    private val _isLoadingActivities = MutableStateFlow(false)
    val isLoadingActivities: StateFlow<Boolean> = _isLoadingActivities

    private val _weeklyMembersWithActivity = MutableStateFlow<List<MemberWithActivity>>(emptyList())
    val weeklyMembersWithActivity: StateFlow<List<MemberWithActivity>> = _weeklyMembersWithActivity
    // --------------------------

    // 일일 활동 데이터 저장 (ActualActivity로 변경)
    private val _dailyActivityData = MutableStateFlow<Map<Int, ActualActivity>>(emptyMap())
    val dailyActivityData: StateFlow<Map<Int, ActualActivity>> = _dailyActivityData

    private val _isLoadingDailyActivity = MutableStateFlow(false)
    val isLoadingDailyActivity: StateFlow<Boolean> = _isLoadingDailyActivity

    fun fetchWeeklyMembersHealthData(members: List<Member>, date: LocalDate) = viewModelScope.launch {
        if (members.isEmpty()) return@launch
        try {
            val startDate = date.startOfWeek().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = date.endOfWeek().format(DateTimeFormatter.ISO_LOCAL_DATE)

            val weeklyMemberActivities = members.map { member ->
                async {
                    repository.getActualActivityForUserRange(member.userPk.toInt(), startDate, endDate)
                }
            }.awaitAll()

            _weeklyMembersWithActivity.value = members.zip(weeklyMemberActivities).map { (member, result) ->
                val activity = result.getOrDefault(ActualActivity(0, 0.0, 0.0, 0))
                MemberWithActivity(member, activity)
            }
            android.util.Log.d("HealthVM_Weekly", "주간 멤버 데이터: ${_weeklyMembersWithActivity.value}")

        } catch (e: Exception) {
            android.util.Log.e("HealthVM_Weekly", "Error fetching weekly health data", e)
            _weeklyMembersWithActivity.value = emptyList()
        }
    }

    fun fetchGroupHealthData(members: List<Member>, date: LocalDate) = viewModelScope.launch {
        if (members.isEmpty()) return@launch
        _isLoadingActivities.value = true
        try {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val memberActivities = members.map { member ->
                async {
                    repository.getActualActivityForUser(member.userPk.toInt(), dateStr)
                }
            }.awaitAll()

            val successfulActivities = memberActivities.mapNotNull { it.getOrNull() }
            android.util.Log.d("HealthVM_Data", "1. API 응답 성공 (성공/${memberActivities.size} 건): $successfulActivities")


            // 전체 통계 계산
            if (successfulActivities.isNotEmpty()) {
                val totalSteps = successfulActivities.sumOf { it.totalSteps }
                val totalCalories = successfulActivities.sumOf { it.totalCalories }
                val totalDuration = successfulActivities.sumOf { it.totalDuration }
                val totalDistances = successfulActivities.sumOf { it.totalDistances }

                _totalStats.value = ActualActivity(
                    totalDuration = totalDuration,
                    totalDistances = totalDistances,
                    totalCalories = totalCalories,
                    totalSteps = totalSteps
                )
            } else {
                _totalStats.value = ActualActivity(0, 0.0, 0.0, 0)
            }
            android.util.Log.d("HealthVM_Data", "2. 그룹 전체 합산 데이터: ${_totalStats.value}")


            // 멤버별 데이터와 활동 데이터 결합
            _membersWithActivity.value = members.zip(memberActivities).map { (member, result) ->
                val activity = result.getOrNull() ?: ActualActivity(0, 0.0, 0.0, 0)
                MemberWithActivity(member, activity)
            }
            android.util.Log.d("HealthVM_Data", "3. UI에 전달될 최종 데이터: ${_membersWithActivity.value}")

        } catch (e: Exception) {
            android.util.Log.e("HealthVM", "Error fetching health data", e)
            _totalStats.value = null
            _membersWithActivity.value = emptyList()
        } finally {
            _isLoadingActivities.value = false
        }
    }


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

    fun loadDailyActivity(userSeq: Int, date: LocalDate? = null) {
        viewModelScope.launch {
            try {
                val targetDate = date ?: LocalDate.now()
                val dateStr = targetDate.toString()
                Log.d("HealthGroupHomeVM", "Loading daily activity for userSeq=$userSeq, date=$dateStr")

                repository.getActualActivityForUser(userSeq, dateStr).onSuccess { data ->
                    Log.d("HealthGroupHomeVM", "Received daily activity for userSeq=$userSeq")
                    _dailyActivityData.value = _dailyActivityData.value + (userSeq to data)

                    Log.d("HealthGroupHomeVM", "👣 걸음수: ${data.totalSteps}")
                    Log.d("HealthGroupHomeVM", "🔥 소모 칼로리: ${data.totalCalories.toInt()} kcal")
                    Log.d("HealthGroupHomeVM", "📏 운동 거리: ${"%.2f".format(data.totalDistances)} km")
                    Log.d("HealthGroupHomeVM", "⏱️ 운동 시간: ${(data.totalDuration / 60000)} 분")
                }.onFailure { e ->
                    Log.e("HealthGroupHomeVM", "Error loading daily activity for userSeq=$userSeq", e)
                }
            } catch (e: Exception) {
                Log.e("HealthGroupHomeVM", "Error loading daily activity for userSeq=$userSeq", e)
            }
        }
    }

    fun loadDailyActivityForMembers(userSeqs: List<Int>, date: LocalDate? = null) {
        // 이미 로딩 중이면 중복 요청 방지
        if (_isLoadingDailyActivity.value) {
            Log.d("HealthGroupHomeVM", "Already loading daily activity, skipping duplicate request")
            return
        }

        Log.d("HealthGroupHomeVM", "loadDailyActivityForMembers called with ${userSeqs.size} members, date=$date")
        _isLoadingDailyActivity.value = true

        viewModelScope.launch {
            try {
                val targetDate = date ?: LocalDate.now()
                val dateStr = targetDate.toString()

                userSeqs.forEach { userSeq ->
                    try {
                        Log.d("HealthGroupHomeVM", "Loading daily activity for userSeq=$userSeq, date=$dateStr")

                        repository.getActualActivityForUser(userSeq, dateStr).onSuccess { data ->
                            Log.d("HealthGroupHomeVM", "Received daily activity for userSeq=$userSeq")
                            _dailyActivityData.value = _dailyActivityData.value + (userSeq to data)

                            // 모든 데이터 로그 출력
                            Log.d("HealthGroupHomeVM", "👣 걸음수: ${data.totalSteps}")
                            Log.d("HealthGroupHomeVM", "🔥 소모 칼로리: ${data.totalCalories.toInt()} kcal")
                            Log.d("HealthGroupHomeVM", "📏 운동 거리: ${"%.2f".format(data.totalDistances)} km")
                            Log.d("HealthGroupHomeVM", "⏱️ 운동 시간: ${(data.totalDuration / 60000)} 분")

                        }.onFailure { e ->
                            Log.e("HealthGroupHomeVM", "Error loading daily activity for userSeq=$userSeq", e)
                        }
                    } catch (e: Exception) {
                        Log.e("HealthGroupHomeVM", "Error loading daily activity for userSeq=$userSeq", e)
                    }
                }
            } finally {
                _isLoadingDailyActivity.value = false
            }
        }
    }

    fun clearDailyActivity() {
        _dailyActivityData.value = emptyMap()
    }

    // 그룹 멤버 한줄평 저장 (userSeq -> comment)
    private val _memberComments = MutableStateFlow<Map<Long, String>>(emptyMap())
    val memberComments: StateFlow<Map<Long, String>> = _memberComments

    fun loadGroupMemberComments(groupSeq: Long) {
        viewModelScope.launch {
            try {
                Log.d("HealthGroupHomeVM", "========== Loading member comments for groupSeq=$groupSeq ==========")
                val response = groupRepo.getGroupMemberComments(groupSeq)

                val comments = response.data?.comments ?: emptyList()
                Log.d("HealthGroupHomeVM", "API Response: ${comments.size} total comments")
                comments.forEach { comment ->
                    Log.d("HealthGroupHomeVM", "  - userSeq=${comment.userSeq}, userName=${comment.userName}, comment=${comment.comment}")
                }

                // userSeq -> comment 맵으로 변환 (comment가 null이 아닌 것만)
                val commentMap = comments
                    .filter { !it.comment.isNullOrBlank() }
                    .associate { it.userSeq to it.comment!! }

                _memberComments.value = commentMap
                Log.d("HealthGroupHomeVM", "Final commentMap: $commentMap")
                Log.d("HealthGroupHomeVM", "Loaded ${commentMap.size} member comments (non-null)")
            } catch (e: Exception) {
                Log.e("HealthGroupHomeVM", "Error loading member comments: ${e.message}", e)
                _memberComments.value = emptyMap()
            }
        }
    }
}
