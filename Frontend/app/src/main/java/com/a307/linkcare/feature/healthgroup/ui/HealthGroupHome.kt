@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.healthgroup.ui

import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.a307.linkcare.R
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.auth.HealthPermissionViewModel
import com.a307.linkcare.feature.ai.ui.AiSuggestionViewModel
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.sampleHealthToday
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.healthgroup.domain.WeeklyGroupGoalResponse
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel.SyncState
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import com.a307.linkcare.common.component.atoms.DatePagerRow
import com.a307.linkcare.common.component.atoms.SummaryChipList
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.commongroup.domain.GoalCriteria
import com.a307.linkcare.feature.commongroup.domain.MemberDetailInfo
import com.a307.linkcare.feature.healthgroup.HealthGroupHomeViewModel
import com.a307.linkcare.feature.mypage.domain.GroupCharacterResponse
import com.a307.linkcare.feature.mypage.MyPageViewModel
import kotlin.collections.get

/* ---------- 날짜 유틸 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
val KOR_WEEK = WeekFields.of(DayOfWeek.MONDAY, 1)

@RequiresApi(Build.VERSION_CODES.O)
val DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREA)

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.weekOfMonth(): Int = this.get(KOR_WEEK.weekOfMonth())

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.korLabel(today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(today, this).toInt()
    val suffix = when (diff) {
        0 -> " (오늘)"
        -1 -> " (어제)"
        1 -> " (내일)"
        else -> ""
    }
    return "${this.format(DATE_FMT)}$suffix"
}

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.toEpochMilliAtLocal(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@RequiresApi(Build.VERSION_CODES.O)
private fun epochMilliToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthGroupHome(
    groupSeq: Long,
    modifier: Modifier = Modifier,
    vm: MyGroupsViewModel = hiltViewModel(),
    goalVm: HealthGroupHomeViewModel = hiltViewModel(),
    aiSuggestionViewModel: AiSuggestionViewModel = hiltViewModel(),  // ⑥ AI 제안 추가
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    healthPermissionViewModel: HealthPermissionViewModel = hiltViewModel(),
    syncViewModel: HealthSyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    // ExerciseSessionManager 주입 (Singleton via EntryPoint)
    val exerciseSessionManager = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.a307.linkcare.feature.caregroup.ExerciseSessionEntryPoint::class.java
        ).exerciseSessionManager()
    }

    val permissionState by healthPermissionViewModel.state.collectAsState()

    // 운동 중인 사용자 ID 목록
    val exercisingUserIds by exerciseSessionManager.exercisingUserIds.collectAsState()

    // 화면 들어올 때 한 번 권한 상태 체크
    LaunchedEffect(Unit) {
        healthPermissionViewModel.checkPermissions()
    }

    // 동기화 상태 관찰
    val syncState by syncViewModel.syncState.collectAsState()

    // Pull-to-Refresh 상태
    var isRefreshing by remember { mutableStateOf(false) }
    val groupChars by myPageViewModel.groupCharacters.collectAsStateWithLifecycle()
    val groupLoad by myPageViewModel.groupCharacterLoading.collectAsStateWithLifecycle()
    val groupError by myPageViewModel.groupCharacterError.collectAsStateWithLifecycle()

    val characterMap = remember(groupChars) {
        groupChars.associateBy({ it.userId }, { it })
    }
    LaunchedEffect(characterMap) {
        Log.d("HealthGroupHome", "characterMap keys = ${characterMap.keys}")
    }

    LaunchedEffect(groupSeq) {
        myPageViewModel.loadGroupCharacters(groupSeq)
    }

    when {
        groupLoad -> CircularProgressIndicator()

        groupError != null -> Text("에러: $groupError")

        groupChars.isNotEmpty() -> {
            Column {
                Text("그룹 캐릭터 ${groupChars.size}명:")

                groupChars.forEach { char ->
                    Text("• ${char.userName} / ${char.petName}")

                    AsyncImage(
                        model = char.mainCharacterImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    val today = remember { LocalDate.now() }

    val detail by vm.detail.collectAsStateWithLifecycle()
    val membersResponse = detail?.members ?: emptyList()
    val goalCriteria = detail?.goalCriteria
    val groupCharResponse: List<GroupCharacterResponse> =
        myPageViewModel.groupCharacters.collectAsState().value ?: emptyList()

    // --- ViewModel 상태 구독 ---
    val totalStats by goalVm.totalStats.collectAsStateWithLifecycle()
    val membersWithActivity by goalVm.membersWithActivity.collectAsStateWithLifecycle()
    val isLoadingActivities by goalVm.isLoadingActivities.collectAsStateWithLifecycle()
    val weeklyMembersWithActivity by goalVm.weeklyMembersWithActivity.collectAsStateWithLifecycle()

    val dailyActivityData by goalVm.dailyActivityData.collectAsStateWithLifecycle()
    val isLoadingDailyActivity by goalVm.isLoadingDailyActivity.collectAsStateWithLifecycle()


    // ⑥ AI 빠른 넛지 메시지
    val quickNudgeMessage by aiSuggestionViewModel.quickNudgeMessage.collectAsState()

    // 그룹 멤버 한줄평 수집
    val memberComments by goalVm.memberComments.collectAsState()

    // -------------------------

    val members = remember(membersResponse, characterMap, memberComments) {
        Log.d("HealthGroupHome", "========== Creating members list ==========")
        Log.d("HealthGroupHome", "memberComments available: ${memberComments.size} comments")
        Log.d("HealthGroupHome", "memberComments map: $memberComments")

        membersResponse.map { m ->
            val charInfo = characterMap[m.userSeq]
            val avatarUrl = charInfo?.mainCharacterImageUrl
            val bgUrl = charInfo?.mainBackgroundImageUrl
            val petName = charInfo?.petName

            val bubbleText = memberComments[m.userSeq] ?: "AI 추천 메시지 준비중..."
            Log.d("HealthGroupHome", "Member ${m.userName} (userSeq=${m.userSeq}): bubbleText='$bubbleText'")

            Member(
                name = m.userName,
                avatarRes = R.drawable.char_bear_1,
                isLeader = m.isLeader,
                progresses = 0,
                goal = detail?.goalCriteria?.minStep ?: 30000,
                bubbleText = bubbleText,
                userPk = m.userSeq
            )
        }
    }

    val mySeq = vm.myUserSeq
    val groupName = detail?.groupName ?: ""
    val isLeader = membersResponse.any { it.userSeq == mySeq && it.isLeader }

    // 최소 기준 기반 초기 목표 (없으면 0)
    val initialGoal = detail?.goalCriteria?.minStep?.toInt() ?: 0

    // AI 목표 상태
    var aiGoals by remember { mutableStateOf<WeeklyGroupGoalResponse?>(null) }
    var currentMetricIndex by remember { mutableStateOf(0) }
    var selectedMetricForSave by remember { mutableStateOf(GoalMetricType.STEPS) }

    val goalState by goalVm.goalState

    // --- 실제 데이터로 교체 ---
    var totalGroupGoal by rememberSaveable { mutableStateOf(initialGoal) }

    // 현재 metric 타입에 따른 진행도 계산
    val currentMetricType = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals?.selectedMetricType ?: "STEPS"
    val todayGroupProgress = when (currentMetricType) {
        "STEPS" -> (totalStats?.totalSteps ?: 0).toDouble()
        "KCAL" -> (totalStats?.totalCalories ?: 0.0)
        "DURATION" -> ((totalStats?.totalDuration ?: 0) / 60000).toDouble()  // 밀리초 → 분
        "DISTANCE" -> (totalStats?.totalDistances ?: 0.0) / 1000 // m -> km 변환
        else -> (totalStats?.totalSteps ?: 0).toDouble()
    }

    val todayGroupPercent by remember(totalGroupGoal, todayGroupProgress) {
        mutableStateOf(
            if (totalGroupGoal > 0) {
                (todayGroupProgress / totalGroupGoal.toDouble()).toFloat()
            } else 0f
        )
    }
    val percentText = ((todayGroupPercent * 100).toInt()).coerceIn(0, 100)

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 날짜 관련 상태
    val minDate = remember { today.minusYears(1) }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // 동기화 완료 시 토스트 표시, 새로고침 종료 및 멤버 상세 데이터 다시 로드
    LaunchedEffect(syncState, members, selectedDate) {
        when (syncState) {
            is SyncState.Success -> {
                isRefreshing = false
                Toast.makeText(context, "동기화 완료!", Toast.LENGTH_SHORT).show()

                // HealthMemberDetail 데이터만 다시 로드
                if (members.isNotEmpty()) {
                    val userSeqs = members.map { it.userPk.toInt() }
                    goalVm.loadDailyActivityForMembers(userSeqs, selectedDate)
                }
            }
            is SyncState.Error -> {
                isRefreshing = false
                val error = (syncState as SyncState.Error).message
                Toast.makeText(context, "동기화 실패: $error", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // --- 데이터 요청 트리거 ---
    LaunchedEffect(groupSeq) {
        Log.d("HealthGroupHome", "========== LaunchedEffect triggered for groupSeq=$groupSeq ==========")
        vm.loadGroupDetail(groupSeq)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        goalVm.loadCurrentGoals(groupSeq, today.format(formatter))
        Log.d("HealthGroupHome", "Calling loadGroupMemberComments for groupSeq=$groupSeq")
        goalVm.loadGroupMemberComments(groupSeq)  // 한줄평 로드
    }

    LaunchedEffect(selectedDate, members) {
        goalVm.fetchGroupHealthData(members, selectedDate)
    }

    // 주차가 변경될 때마다 주간 데이터 로드
    LaunchedEffect(selectedDate.get(KOR_WEEK.weekOfWeekBasedYear()), members) {
        if (members.isNotEmpty()) {
            goalVm.fetchWeeklyMembersHealthData(members, selectedDate)
        }
    }

    // 날짜 변경 시 이전 daily activity 클리어
    LaunchedEffect(selectedDate) {
        goalVm.clearDailyActivity()
    }

    // 멤버 변경 시 또는 날짜 변경 시 daily activity 로드
    LaunchedEffect(members, selectedDate) {
        if (members.isNotEmpty()) {
            val userSeqs = members.map { it.userPk.toInt() }
            goalVm.loadDailyActivityForMembers(userSeqs, selectedDate)
        }
    }
    // -------------------------

    // GoalState 변화 감지
    LaunchedEffect(goalState) {
        when (val state = goalState) {
            is HealthGroupHomeViewModel.GoalState.Success -> {
                aiGoals = state.goals

                val metricType = state.goals.selectedMetricType
                currentMetricIndex = when (metricType) {
                    "STEPS" -> 0
                    "KCAL" -> 1
                    "DURATION" -> 2
                    "DISTANCE" -> 3
                    else -> 0
                }

                val currentGoalValue = when (currentMetricIndex) {
                    0 -> state.goals.goalSteps
                    1 -> state.goals.goalKcal.toLong()
                    2 -> state.goals.goalDuration.toLong()
                    3 -> state.goals.goalDistance.toLong()
                    else -> 0
                }
                totalGroupGoal = currentGoalValue.toInt()
            }
            is HealthGroupHomeViewModel.GoalState.Error -> {
                // 에러 발생 시 사용자에게 토스트를 보여주지 않고 조용히 처리
                android.util.Log.e("HealthGroupHome", "목표 조회 실패: ${state.msg}")
            }
            is HealthGroupHomeViewModel.GoalState.Loading -> {
//                android.util.Log.d("HealthGroupHome", "⏳ UI: Loading 상태")
            }
            is HealthGroupHomeViewModel.GoalState.Idle -> {
//                android.util.Log.d("HealthGroupHome", "💤 UI: Idle 상태")
            }
        }
    }

    val leader = remember(members) {
        members.firstOrNull { it.isLeader } ?: members.firstOrNull()
        ?: Member("나", 50, 100, R.drawable.char_bear_1, "", isLeader = true)
    }

    val leaderCharInfo = remember(leader, characterMap) {
        characterMap[leader.userPk]
    }

    val leaderAvatarUrl = leaderCharInfo?.mainCharacterImageUrl
    val leaderBgUrl = leaderCharInfo?.mainBackgroundImageUrl

    var expandedMemberName by remember { mutableStateOf<String?>(null) }

    val dpState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochMilliAtLocal(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = epochMilliToLocalDate(utcTimeMillis)
                return !date.isBefore(minDate) && !date.isAfter(today)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in minDate.year..today.year
            }
        }
    )

    var selectedMember by remember { mutableStateOf<MemberDetailInfo?>(null) }
    val todayHealthByName = remember(selectedDate, membersWithActivity) {
        membersWithActivity.associate { m -> m.memberInfo.name to sampleHealthToday(m.memberInfo) }
    }

    val todayActivityByUserPk = remember(dailyActivityData, isLoadingDailyActivity) {
        if (isLoadingDailyActivity) {
            emptyMap()
        } else {
            dailyActivityData
        }
    }

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf(initialGoal.toString()) }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }

    /** ---------------- 화면 렌더링 ---------------- */
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                // 권한 체크 후 동기화
                when (permissionState) {
                    HealthPermissionViewModel.State.Granted -> {
                        isRefreshing = true
                        syncViewModel.syncTodayData()
                    }
                    else -> {
                        Toast.makeText(context, "먼저 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                        healthPermissionViewModel.requestPermissions(activity)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
        ) {
        Box(modifier = modifier.fillMaxSize()) {

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    Spacer(Modifier.height(8.dp))

                    // 현재 선택된 metric 타입 가져오기
                    val currentMetricType = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals?.selectedMetricType ?: "STEPS"
                    val currentMetric = when (currentMetricType) {
                        "STEPS" -> GoalMetricType.STEPS
                        "KCAL" -> GoalMetricType.KCAL
                        "DURATION" -> GoalMetricType.DURATION
                        "DISTANCE" -> GoalMetricType.DISTANCE
                        else -> GoalMetricType.STEPS
                    }
                    val unit = currentMetric.unit

                    // 현재 metric에 따른 실제 진행도 계산 및 표시
                    val (progressValue, progressText) = when (currentMetricType) {
                        "STEPS" -> {
                            val value = totalStats?.totalSteps ?: 0
                            value to "$value"
                        }
                        "KCAL" -> {
                            val value = totalStats?.totalCalories?.toInt() ?: 0
                            value to "$value"
                        }
                        "DURATION" -> {
                            val value = ((totalStats?.totalDuration ?: 0) / 60000).toInt()  // 밀리초 → 분
                            value to "$value"
                        }
                        "DISTANCE" -> {
                            val valueInKm = (totalStats?.totalDistances ?: 0.0) / 1000 // m -> km 변환
                            val valueInt = valueInKm.toInt()
                            valueInt to String.format("%.2f", valueInKm)
                        }
                        else -> 0 to "0"
                    }

                    ScreenTitleRow(
                        title = "$progressText$unit",
                        subtitle = if (totalGroupGoal > 0) {
                            "목표 $totalGroupGoal$unit 까지\n${percentText}% 달성했어요"
                        } else {
                            "목표 미설정"
                        },
                        onEditClick = {
                            Log.d("HealthGroupHome", "✏️ 목표 수정 버튼 클릭")
                            if (!isLeader) {
                                scope.launch { snackbar.showSnackbar("그룹장만 목표를 수정할 수 있어요.") }
                                return@ScreenTitleRow
                            }

                            goalInput = if (totalGroupGoal > 0) totalGroupGoal.toString() else ""
                            goalError = null
                            showGoalSheet = true
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    HealthGroupHeaderCard(
                        progress = todayGroupPercent.coerceIn(0f, 1f),
                        bgImgRes = R.drawable.background_1,
                        leaderAvatarRes = leader.avatarRes,
                        avatarUrl = leaderAvatarUrl,
                        backgroundUrl = leaderBgUrl,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    HeaderCard(
                        progress = todayGroupPercent,
                        onPrev = {
                            selectedDate = clampDate(
                                selectedDate.minusDays(1),
                                minDate,
                                today
                            )
                        },
                        onNext = {
                            selectedDate = clampDate(
                                selectedDate.plusDays(1),
                                minDate,
                                today
                            )
                        },
                        label = selectedDate.korLabel(),
                        onClick = { showCalendar = true }
                    )
                }

                itemsIndexed(membersWithActivity, key = { _, item -> item.memberInfo.userPk }) { index, item ->
                    val member = item.memberInfo
                    val activity = item.activity

                    val currentMetricType = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals?.selectedMetricType ?: "STEPS"
                    val goals = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals

                    val (progress, goal) = when (currentMetricType) {
                        "DISTANCE" -> {
                            val progressKm = (activity.totalDistances / 1000.0)
                            val weeklyGroupGoal = goals?.goalDistance
                            val numberOfMembers = membersWithActivity.size.coerceAtLeast(1)
                            val goalKm = if (weeklyGroupGoal != null) {
                                (weeklyGroupGoal / numberOfMembers.toDouble() / 7.0)
                            } else {
                                member.goal.toDouble()
                            }
                            (progressKm * 100.0).toInt() to (goalKm * 100.0).toInt() // 100배로 저장 (소수점 2자리 유지)
                        }
                        else -> {
                            val prog = when (currentMetricType) {
                                "STEPS" -> activity.totalSteps
                                "KCAL" -> activity.totalCalories.toInt()
                                "DURATION" -> (activity.totalDuration / 60000).toInt()  // 밀리초 → 분 (목표값과 단위 맞춤)
                                else -> 0
                            }
                            val numberOfMembers = membersWithActivity.size.coerceAtLeast(1)
                            val weeklyGroupGoal = when (currentMetricType) {
                                "STEPS" -> goals?.goalSteps?.toDouble()
                                "KCAL" -> goals?.goalKcal?.toDouble()
                                "DURATION" -> goals?.goalDuration?.toDouble()
                                else -> null
                            }
                            val g = if (weeklyGroupGoal != null) {
                                (weeklyGroupGoal / numberOfMembers / 7).toInt()
                            } else {
                                member.goal
                            }
                            prog to g
                        }
                    }

                    val currentUnit = when (currentMetricType) {
                        "STEPS" -> "걸음"
                        "KCAL" -> "kcal"
                        "DURATION" -> "분"
                        "DISTANCE" -> "km"
                        else -> ""
                    }

                    val charInfo = characterMap[item.memberInfo.userPk]
                    val avatarUrl = charInfo?.mainCharacterImageUrl
                    val bgUrl = charInfo?.mainBackgroundImageUrl
                    val petName = charInfo?.petName
                    val isExercising = member.userPk > 0 && exercisingUserIds.contains(member.userPk)

                    MemberRowHealth(
                        m = member.copy(progresses = progress, goal = goal),
                        avatarUrl = avatarUrl,
                        backgroundUrl = bgUrl,
                        petName = petName,
                        bubbleVisible = expandedMemberName == member.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == member.name) null else member.name
                        },
                        isExercising = isExercising,
                        onClick = {
                            val charInfo = characterMap[member.userPk]
                            selectedMember = MemberDetailInfo(
                                member = member,
                                avatarUrl = charInfo?.mainCharacterImageUrl,
                                backgroundUrl = charInfo?.mainBackgroundImageUrl,
                                petName = charInfo?.petName
                            )
                        },
                        isSelf = (member.userPk == TokenStore(context).getUserPk()),
                        isDistanceMetric = currentMetricType == "DISTANCE",
                        unit = currentUnit,
                        onTapIconClick = {
                            // ⑥ wellness_other_short: AI 빠른 넛지 메시지 생성
                            Log.d("MemberRowHealth", "Tap icon clicked for member: ${member.name}")
                            Toast.makeText(context, "${member.name}님에게 콕 찌르기를 보냈어요!", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                aiSuggestionViewModel.loadQuickWellnessNudgeMessage(groupSeq, member.userPk)
                            }
                        }
                    )
                    if (index < membersWithActivity.lastIndex) {
                        Divider(color = Color(0x11000000))
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    DatePagerRow(
                        label = "${selectedDate.year}년 ${selectedDate.monthValue}월 ${selectedDate.weekOfMonth()}주차",
                        onPrev = {
                            selectedDate = clampDate(
                                selectedDate.minusWeeks(1),
                                minDate,
                                today
                            )
                        },
                        onNext = {
                            selectedDate = clampDate(
                                selectedDate.plusWeeks(1),
                                minDate,
                                today
                            )
                        }
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${groupName} 그룹의 운동을 분석했어요",
                            fontWeight = FontWeight.SemiBold,
                            color = black
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))

                    val currentMetricType = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals?.selectedMetricType ?: "STEPS"
                    val goals = (goalState as? HealthGroupHomeViewModel.GoalState.Success)?.goals

                    val totalWeeklyGroupGoal = when (currentMetricType) {
                        "STEPS" -> goals?.goalSteps?.toInt()
                        "KCAL" -> goals?.goalKcal?.toInt()
                        "DURATION" -> goals?.goalDuration
                        "DISTANCE" -> goals?.goalDistance?.toInt()
                        else -> 0
                    } ?: 0

                    if (totalWeeklyGroupGoal > 0) {
                        // 목표가 설정된 경우 - 기존 카드 표시
                        val weeklyMembersForCard = remember(weeklyMembersWithActivity, goalState) {
                            weeklyMembersWithActivity.map { memberWithActivity ->
                                val memberInfo = memberWithActivity.memberInfo
                                val weeklyActivity = memberWithActivity.activity

                                val weeklyProgress = when (currentMetricType) {
                                    "STEPS" -> weeklyActivity.totalSteps
                                    "KCAL" -> weeklyActivity.totalCalories.toInt()
                                    "DURATION" -> (weeklyActivity.totalDuration / 60000).toInt()  // 밀리초 → 분
                                    "DISTANCE" -> (weeklyActivity.totalDistances / 1000).toInt() // m -> km 변환
                                    else -> 0
                                }

                                memberInfo.copy(
                                    progresses = weeklyProgress,
                                    goal = totalWeeklyGroupGoal
                                )
                            }
                        }

                        val cardTitle = when (currentMetricType) {
                            "STEPS" -> "주간 걸음수 기여도"
                            "KCAL" -> "주간 칼로리 기여도"
                            "DURATION" -> "주간 운동시간 기여도"
                            "DISTANCE" -> "주간 이동거리 기여도"
                            else -> "주간 활동 기여도"
                        }

                        ProgressSummaryCard(
                            title = cardTitle,
                            members = weeklyMembersForCard
                        )
                    } else {
                        // 목표가 없는 경우 - "목표 설정하세요" 멘트
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "목표 설정하세요",
                                    fontSize = 16.sp,
                                    color = Color(0xFF999999),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    val summaryChips = remember(totalStats) {
                        val calories = totalStats?.totalCalories ?: 0.0
                        val steps = totalStats?.totalSteps ?: 0
                        val durationMs = totalStats?.totalDuration ?: 0
                        val durationSec = totalStats?.totalDuration ?: 0
                        val formattedDuration = formatDuration((durationMs / 1000).toInt())

                        listOf(
                            "🔥 ${calories.toInt()} kcal 태웠어요",
                            "👣 ${steps} 걸음 걸었어요",
                            "⏱️ ${formattedDuration} 운동했어요"
                        )
                    }

                    SummaryChipList(
                        items = summaryChips
                    )
                }
            }

            if (isLoadingActivities) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            selectedMember?.let { info ->
                val myCharInfo = characterMap[mySeq]

                HealthMemberDetail(
                    member = info.member,
                    avatarUrl = info.avatarUrl,
                    backgroundUrl = info.backgroundUrl,
                    petName = info.petName,
                    myCharInfo = myCharInfo,
                    today = todayHealthByName[info.member.name] ?: HealthToday(),
                    dailyActivity = todayActivityByUserPk[info.member.userPk.toInt()],
                    groupSeq = groupSeq,
                    onDismiss = { selectedMember = null },
                    isLoading = isLoadingDailyActivity
                )
            }

            if (showCalendar) {
                DatePickerDialog(
                    onDismissRequest = { showCalendar = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                dpState.selectedDateMillis?.let {
                                    selectedDate = epochMilliToLocalDate(it)
                                }
                                showCalendar = false
                            }
                        ) { Text("확인") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCalendar = false }) { Text("취소") }
                    }
                ) {
                    DatePicker(state = dpState, showModeToggle = false)
                }
            }

            if (showGoalSheet) {
                GoalEditSheet(
                    groupSeq = groupSeq,
                    initialText = goalInput,
                    errorText = goalError,
                    aiGoals = aiGoals,
                    currentMetricIndex = currentMetricIndex,
                    goalCriteria = goalCriteria,
                    isLoading = goalState is HealthGroupHomeViewModel.GoalState.Loading,
                    onTextChange = {
                        goalInput = it
                        goalError = null
                    },
                    onMetricChange = { metric ->
                        selectedMetricForSave = metric

                        val newValue = when (metric) {
                            GoalMetricType.STEPS -> aiGoals?.goalSteps?.toString()
                            GoalMetricType.KCAL -> aiGoals?.goalKcal?.toInt()?.toString()
                            GoalMetricType.DURATION -> aiGoals?.goalDuration?.toString()
                            GoalMetricType.DISTANCE -> aiGoals?.goalDistance?.toInt()?.toString()
                        }

                        if (newValue != null) {
                            goalInput = newValue
                            goalError = null
                        }
                    },
                    onDismiss = {
                        showGoalSheet = false
                    },
                    onSave = { metricType ->
                        val parsed = goalInput.toLongOrNull()

                        val minValue: Long = when (metricType) {
                            GoalMetricType.STEPS -> goalCriteria?.minStep?.toLong() ?: 0L
                            GoalMetricType.KCAL -> goalCriteria?.minCalorie?.toLong() ?: 0L
                            GoalMetricType.DURATION -> goalCriteria?.minDuration?.toLong() ?: 0L
                            GoalMetricType.DISTANCE -> goalCriteria?.minDistance?.toLong() ?: 0L
                        }

                        when {
                            goalInput.isEmpty() -> goalError = "값을 입력하세요."
                            parsed == null || parsed <= 0 -> goalError = "양의 정수를 입력하세요."
                            parsed < minValue && minValue > 0 -> {
                                goalVm.saveGoal(
                                    groupSeq = groupSeq,
                                    metricType = metricType.name,
                                    goalValue = minValue
                                )
                                totalGroupGoal = minValue.toInt()
                                showGoalSheet = false
                                scope.launch {
                                    snackbar.showSnackbar("최소 기준 ${minValue}으로 설정되었어요.")
                                }
                            }
                            else -> {
                                goalVm.saveGoal(
                                    groupSeq = groupSeq,
                                    metricType = metricType.name,
                                    goalValue = parsed
                                )
                                totalGroupGoal = parsed.toInt()
                                showGoalSheet = false
                                scope.launch { snackbar.showSnackbar("목표가 저장되었어요.") }
                            }
                        }
                    }
                )
            }
        }
        }
    }
}


/* ---------- 컴포넌트 ---------- */
@Composable
fun ScreenTitleRow(
    title: String,
    subtitle: String,
    onEditClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, color = black)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF666666), lineHeight = 18.sp)
        }
        FilledTonalButton(
            onClick = onEditClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = main,
                contentColor = white
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) { Text("목표 수정") }
    }
}

@Composable
fun HeaderCard(
    progress: Float,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onClick() },
            color = main,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, color = white)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrev) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "이전", tint = white)
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "다음",
                            tint = white
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

enum class GoalMetricType(val label: String, val unit: String) {
    STEPS("걸음수", "걸음"),
    KCAL("칼로리", "kcal"),
    DURATION("운동시간", "분"),
    DISTANCE("이동거리", "km")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditSheet(
    groupSeq: Long,
    initialText: String,
    errorText: String?,
    aiGoals: WeeklyGroupGoalResponse?,
    currentMetricIndex: Int,
    goalCriteria: GoalCriteria?,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onMetricChange: (GoalMetricType) -> Unit,
    onDismiss: () -> Unit,
    onSave: (GoalMetricType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val metrics = GoalMetricType.entries.toList()

    val selectedMetric = remember(aiGoals, currentMetricIndex) {
        if (aiGoals != null) {
            metrics[currentMetricIndex % metrics.size]
        } else {
            GoalMetricType.STEPS
        }
    }

    val suggestedGoal = remember(aiGoals, selectedMetric) {
        aiGoals?.let {
            when (selectedMetric) {
                GoalMetricType.STEPS -> it.goalSteps.toString()
                GoalMetricType.KCAL -> it.goalKcal.toString()
                GoalMetricType.DURATION -> it.goalDuration.toString()
                GoalMetricType.DISTANCE -> it.goalDistance.toString()
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var localMetric by remember(selectedMetric) { mutableStateOf(selectedMetric) }

    val minValue = remember(localMetric, goalCriteria) {
        when (localMetric) {
            GoalMetricType.STEPS -> goalCriteria?.minStep?.toLong() ?: 0
            GoalMetricType.KCAL -> goalCriteria?.minCalorie?.toLong() ?: 0
            GoalMetricType.DURATION -> goalCriteria?.minDuration?.toLong() ?: 0
            GoalMetricType.DISTANCE -> goalCriteria?.minDistance?.toLong() ?: 0
        }
    }

    LaunchedEffect(suggestedGoal) {
        suggestedGoal?.let { onTextChange(it) }
    }

    LaunchedEffect(localMetric) {
        onMetricChange(localMetric)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("그룹 목표 설정", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = black)

            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = localMetric.label,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("목표 유형") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    metrics.forEach { metric ->
                        DropdownMenuItem(
                            text = { Text(metric.label) },
                            onClick = {
                                localMetric = metric
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = initialText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("목표 ${localMetric.label}") },
                placeholder = { Text("숫자만 입력") },
                suffix = { Text(localMetric.unit) },
                singleLine = true,
                trailingIcon = {
                    if (initialText.isNotEmpty()) {
                        IconButton(onClick = { onTextChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "지우기")
                        }
                    }
                },
                isError = errorText != null
            )

            if (errorText != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorText, color = Color(0xFFD32F2F), fontSize = 12.sp)
            }

            if (minValue > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "최소 기준: ${minValue} ${localMetric.unit}",
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("취소") }

                Button(
                    onClick = { onSave(localMetric) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = main, contentColor = white),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("저장") }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun clampDate(
    date: LocalDate,
    min: LocalDate,
    max: LocalDate
): LocalDate = when {
    date.isBefore(min) -> min
    date.isAfter(max) -> max
    else -> date
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0 초"

    val h = seconds / 3600                  // 시간
    val m = (seconds % 3600) / 60           // 분
    val s = seconds % 60                    // 초

    return when {
        h > 0 && m > 0 -> "${h} 시간 ${m} 분"
        h > 0 && m == 0 -> "${h} 시간"
        m > 0 && s > 0 -> "${m} 분 ${s} 초"
        m > 0 && s == 0 -> "${m} 분"
        else -> "${s} 초"
    }
}
