@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.caregroup

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
import com.a307.linkcare.feature.caregroup.data.*;
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel

fun sampleHealthToday(m: Member): HealthToday =
    HealthToday(
        // 💓 혈압
        bloodPressures = List((1..3).random()) {
            val hour = (7..21).random()
            val minute = listOf("00", "15", "30", "45").random()
            BloodPressure(
                time = String.format("%02d:%s", hour, minute),
                sys = (110..135).random(),
                dia = (70..88).random()
            )
        }.sortedBy { it.time },

        // 💧 음수량
        waterLogs = List((3..6).random()) {
            val hour = (8..22).random()
            val minute = listOf("00", "30").random()
            WaterLog(
                time = String.format("%02d:%s", hour, minute),
                amountMl = listOf(150, 200, 250, 300).random()
            )
        }.sortedBy { it.time },
        waterGoalMl = 2000,

        // 😴 수면
        sleepSessions = listOf(
            SleepSession("23:30", "06:50")
        ) + if ((0..1).random() == 1) {
            listOf(SleepSession("13:20", "14:10")) // 낮잠 있을 수도 있음
        } else emptyList(),

        // 🏃 운동
        activitySessions = List((1..3).random()) { idx ->
            ActivitySession(
                sessionId = "${System.currentTimeMillis()}_$idx",
                avgHeartRate = listOf(98, 110, 121, 130).random(),
                calories = listOf(180, 240, 300).random(),
                distance = listOf(1200, 1500, 2000).random(),  // meter
                durationSec = listOf(480, 600, 900).random(),
                timestamp = System.currentTimeMillis() - (idx * 60 * 60 * 1000)
            )
        }
    )

/* ---------- 날짜 유틸 ---------- */
enum class MetricType { ACTIVITY, BLOOD_PRESSURE, SLEEP }

/* ---------- 스크린 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CareGroupHome(
    groupSeq: Long,
    modifier: Modifier = Modifier,
    vm: MyGroupsViewModel = hiltViewModel()
) {
    LaunchedEffect(groupSeq) {
        vm.loadGroupDetail(groupSeq)
    }

   val detail by vm.detail.collectAsState()
    val membersResponse by vm.members.collectAsState()   // groupDetail.members 매핑해둔 StateFlow
    val groupName by vm.groupName.collectAsState()

    val apiMembers = remember(membersResponse) {
        membersResponse.map { m ->
            Member(
                name = m.userName,
                progresses = 10000,                  // TODO: 헬스데이터 나오면 실제 값으로 교체
                goal = 30000,                        // TODO: 그룹 목표 값으로 교체
                avatarRes = R.drawable.char_bear_1,  // TODO: m.mainCharacterBaseImageUrl 매핑
                bubbleText = "컨디션 좋아요!",       // TODO: AI 한 줄 요약 나오면 교체
                isLeader = m.isLeader
            )
        }
    }

    // 더미 fallback
    val demoMembers = remember {
        listOf(
            Member("김싸피(나)", 10001, 30000, R.drawable.char_bear_1, "컨디션 좋아요!", isLeader = true),
            Member("김아이", 13001, 30000, R.drawable.char_bear_2, "오늘은 좀 피곤해요"),
            Member("김에이", 15000, 30000, R.drawable.char_bear_3, "잘 걸었다!"),
            Member("김삼성", 11000, 30000, R.drawable.char_duck_1, "저녁 산책 완료 🐾")
        )
    }

    // 실제로 사용할 멤버 리스트
    val members = if (apiMembers.isNotEmpty()) apiMembers else demoMembers

    val leader = remember(members) {
        members.firstOrNull { it.isLeader } ?: members.first()
    }

    // 날짜/리스트 상태
    val today = remember { LocalDate.now() }
    val minDate = remember { today.minusYears(1) }   // 1년 전
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // 하나의 확장 상태만 유지 (리스트 공통)
    var expandedMemberName by remember { mutableStateOf<String?>(null) }

    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

    var todayGroupPercent by remember { mutableStateOf(0.62f) }   // TODO: API 나오면 교체
    var todayGroupProgress by remember { mutableStateOf(10234) }  // TODO
    var totalGroupGoal by remember { mutableStateOf(20000) }      // TODO

    // 날짜 이동
    val onPrevDay = { selectedDate = selectedDate.minusDays(1) }
    val onNextDay = { selectedDate = selectedDate.plusDays(1) }

    // 주차 변경 시 → 해당 주의 멤버 데이터
    val year = selectedDate.year
    val month = selectedDate.monthValue
    val wom = selectedDate.weekOfMonth()
    LaunchedEffect(year, month, wom, members) {
        val start = selectedDate.startOfWeek()
        val end = selectedDate.endOfWeek()
        // TODO: 해당 주차 health/care 데이터 API 나오면 여기서 교체
        weekMembers = members
    }

    // 날짜 변경 시 → 오늘 멤버 목록
    LaunchedEffect(selectedDate, weekMembers) {
        // TODO: 선택한 날짜 데이터만 필터링 하려면 여기에서 필터
        todayMembers = weekMembers
    }

    // 날짜 선택 다이얼로그 상태
    // 날짜 선택 다이얼로그용 상태
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

    // 날짜/지표에 따른 요약
    val summary = remember(selectedDate, metricType) {
        sampleSummary(selectedDate.korLabel(), metricType)
    }

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    val todayHealthByName = remember(selectedDate, todayMembers) {
        todayMembers.associate { m -> m.name to sampleHealthToday(m) }
    }

    Scaffold { _ ->
        Box(modifier = modifier.fillMaxSize()) {

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    Spacer(Modifier.height(8.dp))
                    ScreenTitleOnlyRow(
                        title = "이번주,",
                        txt = "걷기에 더 신경써볼까요?"
                    )
                    Spacer(Modifier.height(8.dp))
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
                        onClick = { showCalendar = true }
                    )
                }

                // 멤버 리스트
                itemsIndexed(todayMembers, key = { index, _ -> index }) { index, m ->
                    MemberRow(
                        m = m,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == m.name) null else m.name
                        },
                        onClick = { selectedMember = m }
                    )

                    // 마지막 아이템이 아닌 경우에만 Divider 표시
                    if (index < todayMembers.lastIndex) {
                        Divider(color = Color(0x11000000))
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    DatePagerRow(
                        label = "${selectedDate.year}년 ${selectedDate.monthValue}월 ${selectedDate.weekOfMonth()}주차",
                        onPrev = { selectedDate = selectedDate.minusWeeks(1) },
                        onNext = { selectedDate = selectedDate.plusWeeks(1) }
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
                            members = todayMembers,
                            totalSteps = todayGroupProgress,
                            totalGoal = totalGroupGoal,
                            modifier = Modifier
                        )
                    }
                } else {
                    item {
                        Spacer(Modifier.height(6.dp))
                        val chartData = remember(selectedDate, weekMembers, metricType) {
                            buildWeeklyChartData(metricType, selectedDate, weekMembers)
                        }
                        MetricChartCard(
                            title = when (metricType) {
                                MetricType.BLOOD_PRESSURE -> "주간 혈압 추이"
                                MetricType.SLEEP -> "주간 수면 시간 추이"
                                else -> ""
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
                    SummaryChipList(
                        items = buildSummaryChips(metricType, summary)
                    )
                }
            }

            selectedMember?.let { m ->
                CareMemberDetail(
                    member = m,
                    today = todayHealthByName[m.name] ?: HealthToday(),
                    onDismiss = { selectedMember = null }
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
fun buildSummaryChips(type: MetricType, s: CareWeekSummary): List<String> = when (type) {
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

    MetricType.SLEEP -> listOfNotNull(
        s.sleepMinutes?.let { "😴 총 수면 ${it}분" },
        s.deepMinutes?.let { "🌙 깊은 수면 ${it}분" },
        s.sleepScore?.let { "⭐ 수면 점수 ${it}점" }
    )
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
        MetricType.BLOOD_PRESSURE to "혈압",
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
