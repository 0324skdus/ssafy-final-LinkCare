@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.caregroup

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
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.R
import java.time.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.feature.caregroup.domain.CareWeekSummary
import com.a307.linkcare.feature.caregroup.domain.Tap
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.caregroup.data.*
import com.a307.linkcare.feature.caregroup.domain.CareGroupHomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.healthgroup.formatDuration

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.a307.linkcare.feature.auth.HealthPermissionViewModel
import com.a307.linkcare.feature.caregroup.domain.SleepStatisticsResponse
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel
import com.a307.linkcare.sdk.health.presentation.HealthSyncViewModel.SyncState
fun sampleHealthToday(m: Member): HealthToday =
    HealthToday(
        // 💓 혈압
        bloodPressures = List((1..3).random()) {
            val hour = (7..21).random()
            val minute = listOf("00", "15", "30", "45").random()
            BloodPressure(
                startTime = String.format("2025-01-15T%02d:%s:00", hour, minute),
                systolic = (110..135).random().toFloat(),
                diastolic = (70..88).random().toFloat()
            )
        }.sortedBy { it.startTime },

        // 💧 음수량
        waterIntakes = List((3..6).random()) {
            val hour = (8..22).random()
            val minute = listOf("00", "30").random()
            WaterIntake(
                startTime = String.format("2025-01-15T%02d:%s:00", hour, minute),
                amount = listOf(150f, 200f, 250f, 300f).random()
            )
        }.sortedBy { it.startTime },
        waterGoalMl = 2000,

        // 😴 수면
        sleeps = listOf(
            Sleep(startTime = "2025-01-14T23:30:00", endTime = "2025-01-15T06:50:00", duration = 440)
        ) + if ((0..1).random() == 1) {
            listOf(Sleep(startTime = "2025-01-15T13:20:00", endTime = "2025-01-15T14:10:00", duration = 50)) // 낮잠
        } else emptyList(),

        // 🏃 운동
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
enum class MetricType { ACTIVITY, BLOOD_PRESSURE, SLEEP }

/* ---------- 스크린 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CareGroupHome(
    healthPermissionViewModel: HealthPermissionViewModel = hiltViewModel(),
    groupSeq: Long,
    modifier: Modifier = Modifier,
    groupViewModel: MyGroupsViewModel = hiltViewModel(),
    healthViewModel: CareGroupHomeViewModel = hiltViewModel(),
    syncViewModel: HealthSyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    val permissionState by healthPermissionViewModel.state.collectAsState()
    val stepUiState by healthViewModel.stepUiState.collectAsState()

    // 화면 들어올 때 한 번 권한 상태 체크
    LaunchedEffect(Unit) {
        healthPermissionViewModel.checkPermissions()
    }

    // 그룹 상세 정보 로드
    LaunchedEffect(groupSeq) {
        groupViewModel.loadGroupDetail(groupSeq)
        healthViewModel.loadGroupStepStatistics(groupSeq)
    }

//    val context = LocalContext.current
    val currentUserName = remember {
        com.a307.linkcare.feature.auth.data.TokenStore(context).getName() ?: "나"
    }

    // 동기화 상태 관찰
    val syncState by syncViewModel.syncState.collectAsState()

    // 동기화 완료 시 토스트 표시
    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> {
                Toast.makeText(context, "동기화 완료!", Toast.LENGTH_SHORT).show()
            }
            is SyncState.Error -> {
                val error = (syncState as SyncState.Error).message
                Toast.makeText(context, "동기화 실패: $error", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    val detail by groupViewModel.detail.collectAsState()
    val membersResponse by groupViewModel.members.collectAsState()   // groupDetail.members 매핑해둔 StateFlow
    val groupName by groupViewModel.groupName.collectAsState()
    val totalStats by groupViewModel.totalActivityStats.collectAsState()
    val healthData by healthViewModel.healthData.collectAsState()
    val healthFeedbackMap by healthViewModel.healthFeedbackMap.collectAsState()

    // 멤버 변경 시 health feedback 로드
    LaunchedEffect(membersResponse) {
        if (membersResponse.isNotEmpty()) {
            val userSeqs = membersResponse.map { it.userSeq.toInt() }
            healthViewModel.loadHealthFeedbackForMembers(userSeqs)
        }
    }

    val members = remember(membersResponse, stepUiState, healthFeedbackMap) {
        Log.d("CareGroupHome", "Converting ${membersResponse.size} members from API")

        // 🔹 /step-statistics에서 가져온 값들
        val totalSteps = stepUiState.goal                // = totalSteps
        val stepsByUserSeq = stepUiState.members.associateBy(
            keySelector = { it.userSeq },               // DTO: userSeq
            valueTransform = { it.steps }               // DTO: steps
        )

        // step 통계가 아직 없을 때는 기존 더미값 유지할지 여부
        val hasStats = totalSteps > 0 && stepsByUserSeq.isNotEmpty()

        membersResponse.map { m ->
            Log.d("CareGroupHome", "Member: ${m.userName}, userSeq=${m.userSeq}")

            val steps = if (hasStats) {
                stepsByUserSeq[m.userSeq] ?: 0
            } else {
                0
            }

            val goal = if (hasStats) totalSteps else 0

            // 건강 피드백 가져오기
            val feedback = healthFeedbackMap[m.userSeq.toInt()]
            val status = feedback?.status?.name ?: "GOOD"
            val summary = feedback?.summary ?: "활동이 안정적이에요."

            Member(
                name = m.userName,
                progresses = steps,
                goal = goal,
                avatarRes = R.drawable.char_bear_1,   // TODO: m.mainCharacterBaseImageUrl 매핑
                bubbleText = "",
                isLeader = m.isLeader,
                userPk = m.userSeq,
                status = status,
                summary = summary
            )
        }
    }


    // 날짜/리스트 상태
    val today = remember { LocalDate.now() }
    val minDate = remember { today.minusYears(1) }   // 1년 전
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // 하나의 확장 상태만 유지 (리스트 공통)
    var expandedMemberName by remember { mutableStateOf<String?>(null) }

    // 주차/일자 데이터 (데모)
    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

    var todayGroupPercent by remember { mutableStateOf(0.62f) }   // TODO: API 나오면 교체
    var todayGroupProgress by remember { mutableStateOf(10234) }  // TODO
    var totalGroupGoal by remember { mutableStateOf(20000) }      // TODO

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
        val start = selectedDate.startOfWeek()
        val end = selectedDate.endOfWeek()
        // TODO: 주차별 데이터 필터링이 필요하면 여기서 처리
        weekMembers = members
    }

    // 날짜 변경 시
    LaunchedEffect(selectedDate, weekMembers) {
        // TODO: 날짜별 데이터 필터링이 필요하면 여기서 처리
        todayMembers = weekMembers
    }

    // 멤버 건강 데이터 로드
    LaunchedEffect(todayMembers) {
        val userPks = todayMembers.mapNotNull {
            if (it.userPk > 0) it.userPk.toInt() else null
        }
        Log.d("CareGroupHome", "Loading health data for ${userPks.size} members: $userPks")
        if (userPks.isNotEmpty()) {
            healthViewModel.loadHealthDataForMembers(userPks)
        } else {
            Log.d("CareGroupHome", "No valid userPks to load health data")
        }
    }

    // 다이얼로그는 리스트 바깥에서 remember
    val dpState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochMilliAtLocal(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = epochMilliToLocalDate(utcTimeMillis)
                // minDate ~ today 사이만 선택 가능
                return !date.isBefore(minDate) && !date.isAfter(today)
            }

            override fun isSelectableYear(year: Int): Boolean {
                // 연도도 같이 제한
                return year in minDate.year..today.year
            }
        }
    )

    val taps = remember {
        mutableStateListOf(
            Tap(1, "싸피님", "싸피님 3일째 수면이 부족합니다.", focused = true),
            Tap(2, "아이님", "아이님 심박수가 낮습니다.")
        )
    }

    // 지표 탭 상태
    var metricType by rememberSaveable { mutableStateOf(MetricType.ACTIVITY) }

    // 날짜 또는 지표가 바뀔 때마다 요약 갱신
    val summary = remember(selectedDate, metricType) {
        sampleSummary(selectedDate.korLabel(), metricType)
    }

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    val todayHealthByName = remember(selectedDate, todayMembers, healthData) {
        Log.d("CareGroupHome", "healthData size = ${healthData.size}, keys = ${healthData.keys}")
        todayMembers.associate { m ->
            Log.d("CareGroupHome", "Looking for userPk=${m.userPk.toInt()} for ${m.name}")
            val health = if (m.userPk > 0) {
                val apiData = healthData[m.userPk.toInt()]
                Log.d("CareGroupHome", "API data for ${m.name}: ${if (apiData != null) "FOUND" else "NOT FOUND"}")
                apiData ?: sampleHealthToday(m)
            } else {
                Log.d("CareGroupHome", "userPk is 0 for ${m.name}, using dummy data")
                sampleHealthToday(m)
            }
            m.name to health
        }
    }

    val header = healthViewModel.headerMessage

    val sleepStatistics by healthViewModel.sleepStatistics.collectAsState()

    LaunchedEffect(Unit) {
        healthViewModel.loadHeader(groupSeq, today)
    }

    LaunchedEffect(metricType, selectedDate) {
        if (metricType == MetricType.SLEEP) {
            val start = selectedDate.startOfWeek()
            val end = selectedDate.endOfWeek()
            healthViewModel.loadSleepStatistics(groupSeq, start, end)
        }
    }


    Scaffold(
        floatingActionButton = {
            CareGroupFab(
                permissionState = permissionState,
                onRequestPermission = {
                    healthPermissionViewModel.requestPermissions(activity)
                },
                onSync = {
                    syncViewModel.syncTodayData()
                    Toast.makeText(context, "건강 데이터 동기화 중...", Toast.LENGTH_SHORT).show()
                }
            )
        }
    )  { _ ->
        LazyColumn(Modifier.fillMaxSize()) {

            if (header != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ScreenTitleOnlyRow(
                        title = "이번주,",
                        txt = header
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

                item {
                    NudgeHost(
                        items = taps,
                        key = { it.id },
                        message = { it.text },
                        nameToBold = { it.name },
                        focused = { it.focused },
                        onDismiss = { taps.remove(it) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    HeaderCard(
                        progress = todayGroupPercent,
                        onPrev = onPrevDay,
                        onNext = onNextDay,
                        label = selectedDate.korLabel(),
                        onClick = { showCalendar = true } // 클릭으로 달력 열기
                    )
                }

                // 멤버 리스트
                itemsIndexed(todayMembers, key = { index, _ -> index }) { index, m ->
                    MemberRowCare(
                        m = m,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == m.name) null else m.name
                        },
                        onClick = { selectedMember = m },
                        isSelf = (m.name == currentUserName)
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

                // 운동 탭일 때만 진행률 카드
                if (metricType == MetricType.ACTIVITY) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        ProgressSummaryCard(
                            members = todayMembers,
                            totalSteps = stepUiState.goal,
                            totalGoal = stepUiState.goal,
                            modifier = Modifier
                        )
                    }
                } else {
                    // 혈압/수면: 멤버별 라인 + 전체 평균 라인
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
                                MetricType.BLOOD_PRESSURE -> "주간 혈압 추이"
                                MetricType.SLEEP -> "주간 수면 시간 추이"
                                else -> "" // 사용 안됨
                            },
                            unit = when (metricType) {
                                MetricType.BLOOD_PRESSURE -> "mmHg"
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

            selectedMember?.let { m ->

                // 🔥 디테일 열릴 때 해당 멤버 건강 데이터 한 번 더 로드
                LaunchedEffect(m.userPk) {
                    if (m.userPk > 0) {
                        Log.d("CareGroupHome", "Reload health data for userPk=${m.userPk}")
                        healthViewModel.loadHealthData(m.userPk.toInt())
                    }
                }

                CareMemberDetail(
                    member = m,
                    today = todayHealthByName[m.name] ?: HealthToday(),
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

/* ---------- 컴포넌트 ---------- */
@Composable
private fun ScreenTitleRow(
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
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = main, contentColor = white),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) { Text("목표 수정") }
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
                .clickable { onClick() }, // 클릭으로 달력 오픈
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
    txt: String
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

    MetricType.BLOOD_PRESSURE -> listOfNotNull(
        if (s.systolicAvg != null && s.diastolicAvg != null)
            "🩺 평균 혈압 ${s.systolicAvg}/${s.diastolicAvg} mmHg" else null,
        s.morningBp?.let { (sys, dia) -> "🌅 아침 ${sys}/${dia} mmHg" },
        s.eveningBp?.let { (sys, dia) -> "🌇 저녁 ${sys}/${dia} mmHg" }
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
    MetricType.BLOOD_PRESSURE -> CareWeekSummary(
        dateLabel = dateLabel,
        systolicAvg = 118,
        diastolicAvg = 76,
        morningBp = 120 to 78,
        eveningBp = 116 to 74
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

@Composable
private fun CareGroupFab(
    permissionState: HealthPermissionViewModel.State,
    onRequestPermission: () -> Unit,
    onSync: () -> Unit
) {
    FloatingActionButton(
        onClick = {
            when (permissionState) {
                HealthPermissionViewModel.State.Granted -> onSync()
                HealthPermissionViewModel.State.NeedPermission -> onRequestPermission()
                is HealthPermissionViewModel.State.Error -> onRequestPermission()
                HealthPermissionViewModel.State.Idle -> onRequestPermission()
            }
        },
        containerColor = main,
        contentColor = white
    ) {
        Icon(Icons.Default.Refresh, contentDescription = "동기화")
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