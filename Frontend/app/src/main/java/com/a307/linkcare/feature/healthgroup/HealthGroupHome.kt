@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.healthgroup

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.sampleHealthToday
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.commongroup.domain.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/* ---------- 날짜 유틸 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
val KOR_WEEK = WeekFields.of(DayOfWeek.SUNDAY, 1)
@RequiresApi(Build.VERSION_CODES.O)
val DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREA)

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.weekOfMonth(): Int = this.get(KOR_WEEK.weekOfMonth())
@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.startOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 1) // 일요일
@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.endOfWeek(): LocalDate = this.with(KOR_WEEK.dayOfWeek(), 7)  // 토요일

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
    vm: MyGroupsViewModel = hiltViewModel()
) {
    LaunchedEffect(groupSeq) {
        vm.loadGroupDetail(groupSeq)
    }

    val detail by vm.detail.collectAsState()   // 전체 그룹 상세 정보
    val membersResponse = detail?.members ?: emptyList()
    val totalStats by vm.totalActivityStats.collectAsState()

    val members = remember(membersResponse) {
        membersResponse.map { m ->
            Member(
                name = m.userName,
                avatarRes = R.drawable.char_bear_1, // TODO: m.mainCharacterBaseImageUrl 적용 예정
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

    // 목표 설정
    val initialGoal = detail?.goalCriteria?.minStep?.toInt() ?: 0

    val leader = remember(members) {
        members.firstOrNull { it.isLeader } ?: members.firstOrNull()
        ?: Member("나", 0, 0, R.drawable.char_bear_1, "", isLeader = true)
    }

    val today = remember { LocalDate.now() }
    val minDate = remember { today.minusYears(1) }   // 1년 전
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    var expandedMemberName by remember { mutableStateOf<String?>(null) }
    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

    var todayGroupProgress by remember { mutableStateOf(10234) }
    var totalGroupGoal by rememberSaveable { mutableStateOf(initialGoal) }

    val todayGroupPercent by remember(totalGroupGoal, todayGroupProgress) {
        mutableStateOf(if (totalGroupGoal > 0) (todayGroupProgress / totalGroupGoal.toFloat()) else 0f)
    }
    val percentText = ((todayGroupPercent * 100).toInt()).coerceIn(0, 100)

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

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf(initialGoal.toString()) }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }

    /** ---------------- 화면 렌더링 ---------------- */
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        Box(modifier = modifier.fillMaxSize()) {

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    Spacer(Modifier.height(8.dp))
                    ScreenTitleRow(
                        title = "$todayGroupProgress",
                        subtitle = "목표 ${if (totalGroupGoal > 0) totalGroupGoal else "미설정"} 까지\n${percentText}% 달성했어요",
                        onEditClick = {
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
                    MemberRow(
                        m = m,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName = if (expandedMemberName == m.name) null else m.name
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
                    initialText = goalInput,
                    errorText = goalError,
                    onTextChange = {
                        goalInput = it
                        goalError = null
                    },
                    onDismiss = { showGoalSheet = false },
                    onSave = {
                        val parsed = goalInput.toIntOrNull()
                        when {
                            goalInput.isEmpty() -> goalError = "값을 입력하세요."
                            parsed == null || parsed <= 0 -> goalError = "양의 정수를 입력하세요."
                            parsed < 1000 -> goalError = "최소 1,000 이상을 권장해요."
                            else -> {
                                totalGroupGoal = parsed
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
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = main, contentColor = white),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditSheet(
    initialText: String,
    errorText: String?,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Text("그룹 목표 설정", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = black)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = initialText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("목표 걸음수(예: 20000)") },
                placeholder = { Text("숫자만 입력") },
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

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("취소") }

                Button(
                    onClick = onSave,
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
