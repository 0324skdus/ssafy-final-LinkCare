@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.caregroup.ui.home

import android.app.Activity
import android.os.Build
import androidx.annotation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.R
import java.time.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.feature.caregroup.data.model.request.CareWeekSummary
import com.a307.linkcare.feature.commongroup.domain.model.Member
import com.a307.linkcare.feature.caregroup.data.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.a307.linkcare.feature.commongroup.ui.home.MyGroupsViewModel
import com.a307.linkcare.feature.healthgroup.ui.home.formatDuration

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.a307.linkcare.feature.ai.ui.AiSuggestionViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.a307.linkcare.common.component.molecules.care.MetricChartCard
import com.a307.linkcare.common.component.molecules.health.ProgressSummaryCard
import com.a307.linkcare.common.component.molecules.memberrow.MemberRowCare
import com.a307.linkcare.feature.auth.ui.permission.HealthPermissionViewModel
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.caregroup.ui.detail.BloodPressure
import com.a307.linkcare.feature.caregroup.ui.detail.CareMemberDetail
import com.a307.linkcare.feature.caregroup.ui.detail.DailyActivitySummary
import com.a307.linkcare.feature.caregroup.ui.detail.ExerciseSession
import com.a307.linkcare.common.di.entrypoint.ExerciseSessionEntryPoint
import com.a307.linkcare.feature.caregroup.domain.mapper.buildWeeklyChartData
import com.a307.linkcare.feature.caregroup.domain.mapper.endOfWeek
import com.a307.linkcare.feature.caregroup.domain.mapper.epochMilliToLocalDate
import com.a307.linkcare.feature.caregroup.domain.mapper.korLabel
import com.a307.linkcare.feature.caregroup.domain.mapper.startOfWeek
import com.a307.linkcare.feature.caregroup.domain.mapper.toEpochMilliAtLocal
import com.a307.linkcare.feature.caregroup.domain.mapper.weekOfMonth
import com.a307.linkcare.feature.caregroup.ui.detail.HealthToday
import com.a307.linkcare.feature.caregroup.ui.detail.Sleep
import com.a307.linkcare.feature.caregroup.ui.detail.WaterIntake
import com.a307.linkcare.feature.caregroup.data.model.response.SleepStatisticsResponse
import com.a307.linkcare.feature.commongroup.data.model.request.MemberDetailInfo
import com.a307.linkcare.feature.mypage.ui.mypage.MyPageViewModel
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel.SyncState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokenStoreEntryPoint {
    fun tokenStore(): TokenStore
}
fun sampleHealthToday(m: Member): HealthToday =
    HealthToday(
        // 혈압
        bloodPressures = List((1..3).random()) {
            val hour = (7..21).random()
            val minute = listOf("00", "15", "30", "45").random()
            BloodPressure(
                startTime = String.format("2025-01-15T%02d:%s:00", hour, minute),
                systolic = (110..135).random().toFloat(),
                diastolic = (70..88).random().toFloat()
            )
        }.sortedBy { it.startTime },

        // 음수량
        waterIntakes = List((3..6).random()) {
            val hour = (8..22).random()
            val minute = listOf("00", "30").random()
            WaterIntake(
                startTime = String.format("2025-01-15T%02d:%s:00", hour, minute),
                amount = listOf(150f, 200f, 250f, 300f).random()
            )
        }.sortedBy { it.startTime },
        waterGoalMl = 2000,

        // 수면
        sleeps = listOf(
            Sleep(
                startTime = "2025-01-14T23:30:00",
                endTime = "2025-01-15T06:50:00",
                duration = 440
            )
        ) + if ((0..1).random() == 1) {
            listOf(
                Sleep(
                    startTime = "2025-01-15T13:20:00",
                    endTime = "2025-01-15T14:10:00",
                    duration = 50
                )
            ) // 낮잠
        } else emptyList(),

        // 운동
        dailyActivitySummary = DailyActivitySummary(
            exercises = List((1..3).random()) { idx ->
                ExerciseSession(
                    startTime = "2025-01-15T${String.format("%02d", 7 + idx * 2)}:00:00",
                    endTime = "2025-01-15T${String.format("%02d", 7 + idx * 2 + 1)}:00:00",
                    distance = listOf(1200f, 1500f, 2000f).random(),  // meter
                    calories = listOf(180f, 240f, 300f).random(),
                    meanPulseRate = listOf(98f, 110f, 121f, 130f).random(),
                    duration = listOf(480L, 600L, 900L).random()
                )
            },
            steps = (5000..12000).random()
        )
    )

/* ---------- 날짜 유틸 ---------- */
enum class MetricType { ACTIVITY, SLEEP }

/* ---------- 스크린 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CareGroupHome(
    healthPermissionViewModel: HealthPermissionViewModel = hiltViewModel(),
    groupSeq: Long,
    onReloadRequest: () -> Unit,
    modifier: Modifier = Modifier,
    groupViewModel: MyGroupsViewModel = hiltViewModel(),
    healthViewModel: CareGroupHomeViewModel = hiltViewModel(),
    syncViewModel: HealthSyncViewModel = hiltViewModel(),
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    aiSuggestionViewModel: AiSuggestionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    // ExerciseSessionManager 주입
    val exerciseSessionManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ExerciseSessionEntryPoint::class.java
        ).exerciseSessionManager()
    }

    val permissionState by healthPermissionViewModel.state.collectAsState()
    val stepUiState by healthViewModel.stepUiState.collectAsState()

    // AI 빠른 넛지 메시지
    val quickNudgeMessage by aiSuggestionViewModel.quickNudgeMessage.collectAsState()
    val scope = rememberCoroutineScope()

    // 콕 찌르기 전송 완료 시 토스트 표시
    var lastNudgeMessage by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(quickNudgeMessage) {
        if (quickNudgeMessage.isNotEmpty() && quickNudgeMessage != lastNudgeMessage) {
            lastNudgeMessage = quickNudgeMessage
            Log.d("CareGroupHome", "[콕 찌르기 전송 완료] 메시지: $quickNudgeMessage")
            Toast.makeText(context, "콕 찌르기를 보냈어요!", Toast.LENGTH_LONG).show()
        }
    }

    // 운동 중인 사용자 ID 목록
    val exercisingUserIds by exerciseSessionManager.exercisingUserIds.collectAsState()

    // 화면 들어올 때 한 번 권한 상태 체크
    LaunchedEffect(Unit) {
        healthPermissionViewModel.checkPermissions()
    }

    val groupChars by myPageViewModel.groupCharacters.collectAsStateWithLifecycle()
    val groupLoad by myPageViewModel.groupCharacterLoading.collectAsStateWithLifecycle()
    val groupError by myPageViewModel.groupCharacterError.collectAsStateWithLifecycle()

    val characterMap = remember(groupChars) {
        groupChars.associateBy({ it.userId }, { it })
    }

    // 그룹 상세 정보 로드
    LaunchedEffect(groupSeq) {
        myPageViewModel.loadGroupCharacters(groupSeq)
        groupViewModel.loadGroupDetail(groupSeq)
        healthViewModel.loadGroupStepStatistics(groupSeq)
        healthViewModel.loadGroupMemberComments(groupSeq)
    }

    // TokenStore를 Hilt를 통해 가져옴
    val tokenStore = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TokenStoreEntryPoint::class.java
        ).tokenStore()
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
    val currentUserName = remember {
        tokenStore.getName() ?: "나"
    }

    // 그룹 멤버 한줄평 수집
    val memberComments by healthViewModel.memberComments.collectAsState()

    // 동기화 상태 관찰
    val syncState by syncViewModel.syncState.collectAsState()

    // Pull-to-Refresh 상태
    var isRefreshing by remember { mutableStateOf(false) }

    val membersResponse by groupViewModel.members.collectAsState()
    val groupName by groupViewModel.groupName.collectAsState()
    val totalStats by groupViewModel.totalActivityStats.collectAsState()
    val healthData by healthViewModel.healthData.collectAsState()
    val healthFeedbackMap by healthViewModel.healthFeedbackMap.collectAsState()
    val isLoadingHealthFeedback by healthViewModel.isLoadingHealthFeedback.collectAsState()

    // 날짜/리스트 상태
    val today = remember { LocalDate.now() }
    val minDate = remember { today.minusYears(1) }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // 그룹 상세 정보 로드
    LaunchedEffect(groupSeq) {
        myPageViewModel.loadGroupCharacters(groupSeq)
        groupViewModel.loadGroupDetail(groupSeq)
    }

    // selectedDate 변경 시 주차별 걸음 수 통계 로드
    LaunchedEffect(groupSeq, selectedDate) {
        val startDate = selectedDate.startOfWeek()
        val endDate = selectedDate.endOfWeek()
        healthViewModel.loadGroupStepStatisticsByPeriod(groupSeq, startDate, endDate)
    }

    // 동기화 완료 시 토스트 표시, 새로고침 종료 및 멤버 상세 데이터 다시 로드
    LaunchedEffect(syncState, membersResponse, selectedDate) {
        when (syncState) {
            is SyncState.Success -> {
                isRefreshing = false
                Toast.makeText(context, "동기화 완료!", Toast.LENGTH_SHORT).show()

                // CareMemberDetail 데이터만 다시 로드
                if (membersResponse.isNotEmpty()) {
                    val userPks = membersResponse.mapNotNull {
                        if (it.userSeq > 0) it.userSeq.toInt() else null
                    }
                    if (userPks.isNotEmpty()) {
                        healthViewModel.loadHealthDataForMembers(userPks, selectedDate)
                        healthViewModel.loadHealthFeedbackForMembers(userPks, selectedDate)
                    }
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

    // 날짜 변경 시 이전 health feedback 클리어
    LaunchedEffect(selectedDate) {
        healthViewModel.clearHealthFeedback()
    }

    // 멤버 변경 시 또는 날짜 변경 시 health feedback 로드
    LaunchedEffect(membersResponse, selectedDate) {
        if (membersResponse.isNotEmpty()) {
            val userSeqs = membersResponse.map { it.userSeq.toInt() }
            healthViewModel.loadHealthFeedbackForMembers(userSeqs, selectedDate)
        }
    }

    val members = remember(membersResponse, stepUiState, healthFeedbackMap, memberComments) {
       // 총 거리
        val totalSteps = stepUiState.goal
        val stepsByUserSeq = stepUiState.members.associateBy(
            keySelector = { it.userSeq },
            valueTransform = { it.steps }
        )

        // step 통계가 아직 없을 때는 기존 더미값 유지할지 여부
        val hasStats = totalSteps > 0 && stepsByUserSeq.isNotEmpty()

        membersResponse.map { m ->
            // 캐릭터
            val charInfo = characterMap[m.userSeq]

            val steps = if (hasStats) {
                stepsByUserSeq[m.userSeq] ?: 0
            } else {
                0
            }

            val goal = if (hasStats) totalSteps else 0

            // 건강 피드백 가져오기
            val feedback = healthFeedbackMap[m.userSeq.toInt()]
            val status = if (isLoadingHealthFeedback) {
                "LOADING"
            } else {
                feedback?.status?.name ?: "GOOD"
            }
            val summary = if (isLoadingHealthFeedback) {
                "분석 중입니다!"
            } else {
                feedback?.summary ?: "오늘 하루도 파이팅!"
            }

            val bubbleText = memberComments[m.userSeq] ?: ""

            Member(
                name = m.userName,
                progresses = steps,
                goal = goal,
                avatarRes = R.drawable.char_bear_1,
                bubbleText = bubbleText,
                isLeader = m.isLeader,
                userPk = m.userSeq,
                status = status,
                summary = summary
            )
        }
    }

    var expandedMemberName by remember { mutableStateOf<String?>(null) }

    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

    var todayGroupPercent by remember { mutableStateOf(0.62f) }

    // 날짜 이동
    val onPrevDay = {
        selectedDate = clampDate(
            selectedDate.minusDays(1),
            minDate,
            today
        )
    }

    val onNextDay = {
        selectedDate = clampDate(
            selectedDate.plusDays(1),
            minDate,
            today
        )
    }

    // 주차 변경 시
    val year = selectedDate.year
    val month = selectedDate.monthValue
    val wom = selectedDate.weekOfMonth()
    LaunchedEffect(year, month, wom, members) {
        weekMembers = members
    }

    // 날짜 변경 시
    LaunchedEffect(selectedDate, weekMembers) {
        todayMembers = weekMembers
    }

    // 멤버 건강 데이터 로드 (선택된 날짜 기준)
    LaunchedEffect(todayMembers, selectedDate) {
        val userPks = todayMembers.mapNotNull {
            if (it.userPk > 0) it.userPk.toInt() else null
        }
      if (userPks.isNotEmpty()) {
            healthViewModel.loadHealthDataForMembers(userPks, selectedDate)
        } else {
        }
    }

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



    // 지표 탭 상태
    var metricType by rememberSaveable { mutableStateOf(MetricType.ACTIVITY) }

    // 날짜 또는 지표가 바뀔 때마다 요약 갱신
    val summary = remember(selectedDate, metricType) {
        sampleSummary(selectedDate.korLabel(), metricType)
    }

    var selectedMember by remember { mutableStateOf<MemberDetailInfo?>(null) }
    val todayHealthByName = remember(selectedDate, todayMembers, healthData) {
       todayMembers.associate { m ->
            val health = if (m.userPk > 0) {
                val apiData = healthData[m.userPk.toInt()]
                apiData ?: sampleHealthToday(m)
            } else {
                sampleHealthToday(m)
            }
            m.name to health
        }
    }

    val header = healthViewModel.headerMessage

    val sleepStatistics by healthViewModel.sleepStatistics.collectAsState()

    LaunchedEffect(Unit) {
        healthViewModel.loadHeader(groupSeq)
    }

    LaunchedEffect(metricType, selectedDate) {
        if (metricType == MetricType.SLEEP) {
            val start = selectedDate.startOfWeek()
            val end = selectedDate.endOfWeek()
            healthViewModel.loadSleepStatistics(groupSeq, start, end)
        }
    }

    Scaffold { _ ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                // 권한 체크 후 동기화
                when (permissionState) {
                    HealthPermissionViewModel.State.Granted -> {
                        isRefreshing = true
                        syncViewModel.syncTodayData()

                        onReloadRequest()
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
            LazyColumn(Modifier.fillMaxSize()) {

            if (header != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ScreenTitleOnlyRow(
                        title = "이번주,",
                        txt = header,
                        onClick = {
                            healthViewModel.regenerateHeader(groupSeq)
                            Toast.makeText(context, "문구 만드는 중...", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }



                item {
                    HeaderCard(
                        progress = todayGroupPercent,
                        onPrev = onPrevDay,
                        onNext = onNextDay,
                        label = selectedDate.korLabel(),
                        onClick = { showCalendar = true }
                    )
                }

                // 멤버 리스트
                itemsIndexed(todayMembers, key = { index, _ -> index }) { index, m ->
                    val charInfo = characterMap[m.userPk]
                    val avatarUrl = charInfo?.mainCharacterImageUrl
                    val bgUrl = charInfo?.mainBackgroundImageUrl
                    val petName = charInfo?.petName
                    val isExercising = m.userPk > 0 && exercisingUserIds.contains(m.userPk)

                    MemberRowCare(
                        m = m,
                        avatarUrl = avatarUrl,
                        backgroundUrl = bgUrl,
                        petName = petName,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == m.name) null else m.name
                        },
                        isExercising = isExercising,
                        onClick = {
                            val charInfo = characterMap[m.userPk]
                            selectedMember = MemberDetailInfo(
                                member = m,
                                avatarUrl = charInfo?.mainCharacterImageUrl,
                                backgroundUrl = charInfo?.mainBackgroundImageUrl,
                                petName = charInfo?.petName
                            )
                        },
                        isSelf = (m.userPk == TokenStore(context).getUserPk()),
                        onTapIconClick = {
                            aiSuggestionViewModel.loadQuickNudgeMessage(groupSeq, m.userPk)
                        }
                    )

                    if (index < todayMembers.lastIndex) {
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
                            text = "${(groupName.ifBlank { "케어" })} 그룹의 건강을 분석했어요",
                            fontWeight = FontWeight.SemiBold,
                            color = black
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    MetricTypeTabs(
                        selected = metricType,
                        onSelect = { metricType = it }
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (metricType == MetricType.ACTIVITY) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        ProgressSummaryCard(
                            title = "활동 요약",
                            members = todayMembers,
                            modifier = Modifier
                        )
                    }
                } else {
                    item {
                        Spacer(Modifier.height(6.dp))
                        val chartData = remember(selectedDate, weekMembers, metricType, sleepStatistics) {
                            buildWeeklyChartData(
                                type = metricType,
                                anchorDate = selectedDate,
                                members = weekMembers,
                                sleepStats = sleepStatistics
                            )
                        }
                        MetricChartCard(
                            title = when (metricType) {
                                MetricType.SLEEP -> "주간 수면 시간 추이"
                                else -> ""
                            },
                            unit = when (metricType) {
                                MetricType.SLEEP -> "분"
                                else -> ""
                            },
                            data = chartData
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))

                    val summaryChips = remember(metricType, selectedDate, summary, totalStats, sleepStatistics, members) {
                        if (metricType == MetricType.ACTIVITY) {
                            val calories = totalStats?.totalCalories?.toInt() ?: 0
                            val steps = totalStats?.totalSteps ?: 0
                            val rawDuration = totalStats?.totalDuration ?: 0L
                            val durationSec = (rawDuration.toFloat() / 1000f).toInt()
                            val durationStr = formatDuration(durationSec)
                            val start = selectedDate.startOfWeek()
                            val end = selectedDate.endOfWeek()


                            groupViewModel.loadTotalActivityStats(groupSeq, start, end)

                            listOf(
                                "🔥 ${calories} kcal 태웠어요",
                                "👣 ${steps} 걸음 걸었어요",
                                "⏱️ $durationStr 운동했어요"
                            )
                        } else {
                            buildSummaryChips(
                                type = metricType,
                                s = summary,
                                sleepStats = sleepStatistics,
                                members = members
                            )
                        }
                    }

                    SummaryChipList(
                        items = summaryChips
                    )
                }
            }
        }
        selectedMember?.let { info ->

            LaunchedEffect(info.member.userPk) {
                if (info.member.userPk > 0) {
                    healthViewModel.loadHealthData(info.member.userPk.toInt(), selectedDate)
                }
            }

            val myCharInfo = characterMap.values.find { it.userId == TokenStore(context).getUserPk() }

            CareMemberDetail(
                member = info.member,
                avatarUrl = info.avatarUrl,
                backgroundUrl = info.backgroundUrl,
                petName = info.petName,
                myCharInfo = myCharInfo,
                today = todayHealthByName[info.member.name] ?: HealthToday(),
                groupSeq = groupSeq,
                onDismiss = { selectedMember = null },
                currentUserName = currentUserName
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
    }
}

@Composable
private fun HeaderCard(
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
                        Icon(Icons.Default.ArrowForward, contentDescription = "다음", tint = white)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ---------- 컴포넌트 ---------- */
@Composable
fun ScreenTitleOnlyRow(
    title: String,
    txt: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, color = black)
            Spacer(Modifier.height(4.dp))
            Text(txt, color = Color(0xFF666666), lineHeight = 18.sp)
        }
    }
}

// 요약칩 텍스트를 만들어주는 함수
fun buildSummaryChips(type: MetricType, s: CareWeekSummary, sleepStats: SleepStatisticsResponse?,
                      members: List<Member>): List<String> = when (type) {
    MetricType.ACTIVITY -> listOfNotNull(
        s.calories?.let { "🔥 ${it} kcal 태웠어요" },
        s.steps?.let { "👣 ${it} 걸음 걸었어요" },
        s.activeMinutes?.let { "⏱️ ${it}분 운동했어요" }
    )
    MetricType.SLEEP -> buildSleepSummaryMessages(
        stats = sleepStats,
        members = members
    )
}

fun formatMinutesToHourMin(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}

// 날짜+타입에 맞는 요약 더미 데이터
fun sampleSummary(dateLabel: String, type: MetricType): CareWeekSummary = when (type) {
    MetricType.ACTIVITY -> CareWeekSummary(
        dateLabel = dateLabel,
        steps = 10043,
        calories = 10043,
        activeMinutes = 62
    )
    MetricType.SLEEP -> CareWeekSummary(
        dateLabel = dateLabel,
        sleepMinutes = 423,     // 7시간 3분
        deepMinutes = 98,
        sleepScore = 84
    )
}

@Composable
private fun MetricTypeTabs(
    selected: MetricType,
    onSelect: (MetricType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        MetricType.ACTIVITY to "운동",
        MetricType.SLEEP to "수면"
    )
    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        modifier = modifier.padding(horizontal = 16.dp),
        containerColor = Color(0xFFF6F7FB)
    ) {
        tabs.forEachIndexed { index, (type, label) ->
            Tab(
                selected = selected == type,
                onClick = { onSelect(type) },
                text = { Text(label) }
            )
        }
    }
}

private fun clampDate(
    date: LocalDate,
    min: LocalDate,
    max: LocalDate
): LocalDate = when {
    date.isBefore(min) -> min
    date.isAfter(max) -> max
    else -> date
}

fun buildSleepSummaryMessages(
    stats: SleepStatisticsResponse?,
    members: List<Member>
): List<String> {
    if (stats == null) return emptyList()

    val nameByUserSeq = members.associateBy({ it.userPk }, { it.name })

    val allSleepMinutes: List<Int> = stats.members.flatMap { m ->
        m.dailySleepMinutes
            .filter { it > 0 }
            .map { msToMinutes(it.toLong()) }
    }

    val avgMinutes = if (allSleepMinutes.isNotEmpty()) {
        allSleepMinutes.average().toInt()
    } else 0

    val avgLine = "😴 평균 수면 ${formatMinutesToHourMin(avgMinutes)}"

    data class MemberAvg(val userSeq: Long, val avgMinutes: Int)

    val memberAverages: List<MemberAvg> = stats.members.map { m ->
        val mins = m.dailySleepMinutes
            .filter { it > 0 }
            .map { msToMinutes(it.toLong()) }

        val avg = if (mins.isNotEmpty()) mins.average().toInt() else 0
        MemberAvg(userSeq = m.userSeq, avgMinutes = avg)
    }

    val ranked = memberAverages.sortedByDescending { it.avgMinutes }

    val rankLines = ranked
        .mapIndexed { index, mAvg ->
            val name = nameByUserSeq[mAvg.userSeq] ?: "멤버${index + 1}"
            val sleepText = formatMinutesToHourMin(mAvg.avgMinutes)
            "${index + 1}위 $name · $sleepText"
        }

    return listOf(avgLine) + rankLines
}


fun msToMinutes(ms: Long): Int = (ms / 60000L).toInt()