@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.healthgroup

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.R
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.sampleHealthToday
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.healthgroup.domain.WeeklyGroupGoalResponse
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import com.a307.linkcare.common.component.atoms.DatePagerRow
import com.a307.linkcare.common.component.atoms.SummaryChipList

/* ---------- 날짜 유틸 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
val KOR_WEEK = WeekFields.of(DayOfWeek.SUNDAY, 1)

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
    goalVm: HealthGroupHomeViewModel = hiltViewModel()
) {
    val today = remember { LocalDate.now() }

    val detail by vm.detail.collectAsState()
    val membersResponse = detail?.members ?: emptyList()
    val goalCriteria = detail?.goalCriteria
    val totalStats by vm.totalActivityStats.collectAsState()

    val members = remember(membersResponse) {
        membersResponse.map { m ->
            Member(
                name = m.userName,
                avatarRes = R.drawable.char_bear_1, // TODO: 이미지 적용
                isLeader = m.isLeader,
                progresses = 0,
                goal = detail?.goalCriteria?.minStep ?: 30000,
                bubbleText = "AI 추천 메시지 준비중..."
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

    // 그룹 오늘 진행도 / 목표
    var todayGroupProgress by remember { mutableStateOf(10234) } // TODO: 실제 값으로 교체
    var totalGroupGoal by rememberSaveable { mutableStateOf(initialGoal) }

    val todayGroupPercent by remember(totalGroupGoal, todayGroupProgress) {
        mutableStateOf(
            if (totalGroupGoal > 0) {
                todayGroupProgress / totalGroupGoal.toFloat()
            } else 0f
        )
    }
    val percentText = ((todayGroupPercent * 100).toInt()).coerceIn(0, 100)

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 그룹 상세 + 현재 목표 로드
    LaunchedEffect(groupSeq) {
        vm.loadGroupDetail(groupSeq)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        goalVm.loadCurrentGoals(groupSeq, today.format(formatter))
    }

    // GoalState 변화 감지
    LaunchedEffect(goalState) {
        android.util.Log.d("HealthGroupHome", "📱 UI: GoalState 변화 감지 - ${goalState::class.simpleName}")
        when (val state = goalState) {
            is HealthGroupHomeViewModel.GoalState.Success -> {
                android.util.Log.d("HealthGroupHome", "✅ UI: Success 상태 처리")
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

                android.util.Log.d("HealthGroupHome", "  - 화면에 표시할 목표: $currentGoalValue")
            }
            is HealthGroupHomeViewModel.GoalState.Error -> {
                android.util.Log.e("HealthGroupHome", "❌ 목표 조회 실패: ${state.msg}")
                scope.launch { snackbar.showSnackbar("목표 조회 실패: ${state.msg}") }
            }
            is HealthGroupHomeViewModel.GoalState.Loading -> {
                android.util.Log.d("HealthGroupHome", "⏳ UI: Loading 상태")
            }
            is HealthGroupHomeViewModel.GoalState.Idle -> {
                android.util.Log.d("HealthGroupHome", "💤 UI: Idle 상태")
            }
        }
    }

    val leader = remember(members) {
        members.firstOrNull { it.isLeader } ?: members.firstOrNull()
        ?: Member("나", 50, 100, R.drawable.char_bear_1, "", isLeader = true)
    }

    // 날짜 관련 상태
    val minDate = remember { today.minusYears(1) }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    var expandedMemberName by remember { mutableStateOf<String?>(null) }
    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

//    var todayGroupProgress by remember { mutableStateOf(10234) }
//    var totalGroupGoal by rememberSaveable { mutableStateOf(initialGoal) }
//
//    val todayGroupPercent by remember(totalGroupGoal, todayGroupProgress) {
//        mutableStateOf(if (totalGroupGoal > 0) (todayGroupProgress / totalGroupGoal.toFloat()) else 0f)
//    }
//    val percentText = ((todayGroupPercent * 100).toInt()).coerceIn(0, 100)

    val onPrevDay = { selectedDate = selectedDate.minusDays(1) }
    val onNextDay = { selectedDate = selectedDate.plusDays(1) }


    val year = selectedDate.year
    val month = selectedDate.monthValue
    val wom = selectedDate.weekOfMonth()

    LaunchedEffect(year, month, wom, members) {
        weekMembers = members
    }
    LaunchedEffect(selectedDate, weekMembers) {
        todayMembers = weekMembers
    }

    LaunchedEffect(groupSeq, selectedDate) {
        vm.loadTotalActivityStats(
            groupSeq = groupSeq,
            startDate = selectedDate.startOfWeek(),
            endDate = selectedDate.endOfWeek()
        )
    }

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

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    val todayHealthByName = remember(selectedDate, todayMembers) {
        todayMembers.associate { m -> m.name to sampleHealthToday(m) }
    }

//    val snackbar = remember { SnackbarHostState() }
//    val scope = rememberCoroutineScope()

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf(initialGoal.toString()) }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }

    /** ---------------- 화면 렌더링 ---------------- */
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        Box(modifier = modifier.fillMaxSize()) {

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    Spacer(Modifier.height(8.dp))
                    val currentMetric = GoalMetricType.entries[currentMetricIndex]
                    val unit = currentMetric.unit
                    ScreenTitleRow(
                        title = "$todayGroupProgress$unit",
                        subtitle = "목표 ${if (totalGroupGoal > 0) "$totalGroupGoal$unit" else "미설정"} 까지\n${percentText}% 달성했어요",
                        onEditClick = {
                            android.util.Log.d("HealthGroupHome", "✏️ 목표 수정 버튼 클릭")
                            if (!isLeader) {
                                scope.launch { snackbar.showSnackbar("그룹장만 목표를 수정할 수 있어요.") }
                                return@ScreenTitleRow
                            }

                            if (aiGoals != null) {
                                currentMetricIndex = (currentMetricIndex + 1) % 4
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
                        leaderAvatarRes = leader.avatarRes
                    )
                    Spacer(Modifier.height(12.dp))
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

                itemsIndexed(todayMembers, key = { index, _ -> index }) { index, m ->
                    MemberRowHealth(
                        m = m,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == m.name) null else m.name
                        },
                        onClick = { selectedMember = m }
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
                            text = "${groupName} 그룹의 운동을 분석했어요",
                            fontWeight = FontWeight.SemiBold,
                            color = black
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    ProgressSummaryCard(
                        members = members,
                        totalSteps = todayGroupProgress,
                        totalGoal = totalGroupGoal
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    val summaryChips = remember(totalStats) {
                        val calories = totalStats?.totalCalories ?: 0.0
                        val steps = totalStats?.totalSteps ?: 0
                        val durationSec = totalStats?.totalDuration ?: 0
                        val formattedDuration = formatDuration(durationSec)

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

            selectedMember?.let { m ->
                HealthMemberDetail(
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

                        // AI 목표 세트에서 해당 metric 값만 가져와서 input 업데이트
                        val newValue = when (metric) {
                            GoalMetricType.STEPS -> aiGoals?.goalSteps?.toString()
                            GoalMetricType.KCAL -> aiGoals?.goalKcal?.toInt()?.toString()
                            GoalMetricType.DURATION -> aiGoals?.goalDuration?.toString()
                            GoalMetricType.DISTANCE -> aiGoals?.goalDistance?.toInt()?.toString()
                        }

                        if (newValue != null) {
                            goalInput = newValue     // ← ⭐ 여기서 실제 입력값 변경됨
                            goalError = null
                        }
                    },
                    onDismiss = {
                        showGoalSheet = false
                        goalVm.resetGoalState()
                    },
                    onSave = { metricType ->
                        val parsed = goalInput.toLongOrNull()

                        // 최소 기준 검증 (전부 Long 으로 맞추기)
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
                    },
                    onRefreshClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val dateStr = today.format(formatter)
                        goalVm.generateGroupGoals(
                            groupSeq = groupSeq,
                            requestDate = dateStr
                        )
                    }
                )
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
    goalCriteria: com.a307.linkcare.feature.commongroup.domain.GoalCriteria?,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onMetricChange: (GoalMetricType) -> Unit,
    onDismiss: () -> Unit,
    onSave: (GoalMetricType) -> Unit,
    onRefreshClick: () -> Unit
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("그룹 목표 설정", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = black)

                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = main,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "AI 목표 새로고침",
                            tint = main
                        )
                    }
                }
            }

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
