@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.healthgroup

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.sampleHealthToday
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

/* ---------- 스크린 ---------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthGroupHome(
    membersAll: List<Member>,
    modifier: Modifier = Modifier,
    isLeader: Boolean = true,
    currentGoal: Int?,                       // null이면 미설정 상태
    onSaveGoal: (newGoal: Int) -> Unit = {}  // TODO API 저장
) {
    // 데모 데이터
    val members = remember {
        listOf(
            Member("김싸피(나)", 10001, 30000, R.drawable.char_bear_1, "컨디션 좋아요!", isLeader = true),
            Member("김아이", 13001, 30000, R.drawable.char_bear_2, "오늘은 좀 피곤해요"),
            Member("김에이", 15000, 30000, R.drawable.char_bear_3, "잘 걸었다!"),
            Member("김삼성", 11000, 30000, R.drawable.char_duck_1, "저녁 산책 완료 🐾")
        )
    }
    val leader = remember(members) { members.first { it.isLeader } }

    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // 하나의 확장 상태만 유지 (리스트 공통)
    var expandedMemberName by remember { mutableStateOf<String?>(null) }

    // 주차/일자 데이터 (데모)
    var weekMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var todayMembers by remember { mutableStateOf<List<Member>>(emptyList()) }

    // 진행/목표
    var todayGroupProgress by remember { mutableStateOf(10234) }
    var totalGroupGoal by rememberSaveable { mutableStateOf(currentGoal ?: 0) }
    val todayGroupPercent by remember(totalGroupGoal, todayGroupProgress) {
        mutableStateOf(
            if (totalGroupGoal > 0) (todayGroupProgress / totalGroupGoal.toFloat()) else 0f
        )
    }
    val percentText = ((todayGroupPercent * 100).toInt()).coerceIn(0, 100)

    // 날짜 이동
    val onPrevDay = { selectedDate = selectedDate.minusDays(1) }
    val onNextDay = { selectedDate = selectedDate.plusDays(1) }

    // 주차 변경 시
    val year = selectedDate.year
    val month = selectedDate.monthValue
    val wom = selectedDate.weekOfMonth()
    LaunchedEffect(year, month, wom) {
        val start = selectedDate.startOfWeek()
        val end = selectedDate.endOfWeek()
        // TODO API 교체
        weekMembers = membersAll.ifEmpty { members }
    }

    // 날짜 변경 시
    LaunchedEffect(selectedDate, weekMembers) {
        // TODO API 교체
        todayMembers = weekMembers.ifEmpty { members }
    }

    // 날짜 선택 다이얼로그
    val dpState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochMilliAtLocal()
    )

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    val todayHealthByName = remember(selectedDate, todayMembers) {
        todayMembers.associate { m -> m.name to sampleHealthToday(m) }
    }

    // 스낵바/코루틴
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 목표 수정 바텀시트 상태
    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf(if (currentGoal != null) currentGoal.toString() else "") }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }
    var currentMetricType by rememberSaveable { mutableStateOf(MetricType.STEPS) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        Box(modifier = modifier.fillMaxSize()) {

            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    Spacer(Modifier.height(8.dp))
                    ScreenTitleRow(
                        title = "$todayGroupProgress",
                        subtitle = "목표 ${if (totalGroupGoal > 0) "$totalGroupGoal${currentMetricType.unit}" else "미설정"}까지\n${percentText}% 달성했어요",
                        onEditClick = {
                            if (!isLeader) {
                                scope.launch { snackbar.showSnackbar("그룹장만 목표를 수정할 수 있어요.") }
                                return@ScreenTitleRow
                            }
                            // 미설정이면 빈 값으로, 설정되어 있으면 해당 값으로
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

                // 멤버 리스트(선택 날짜 기준)
                items(todayMembers, key = { it.name }) { m ->
                    MemberRow(
                        m = m,
                        bubbleVisible = expandedMemberName == m.name,
                        onAvatarToggle = {
                            expandedMemberName =
                                if (expandedMemberName == m.name) null else m.name
                        },
                        onClick = { selectedMember = m }
                    )
                    Divider(color = Color(0x11000000))
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
                            text = "만수무강 그룹의 운동을 분석했어요",
                            fontWeight = FontWeight.SemiBold,
                            color = black
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    ProgressSummaryCard(
                        members = membersAll,
                        totalSteps = todayGroupProgress,
                        totalGoal = totalGroupGoal,
                        modifier = Modifier
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SummaryChipList(
                        items = listOf(
                            "🔥 10043 kcal 태웠어요",
                            "👣 10043 걸음 걸었어요",
                            "⏱️ 10043 시간 운동했어요"
                        )
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

            // ▼ 목표 수정 바텀시트
            if (showGoalSheet) {
                GoalEditSheet(
                    initialText = goalInput,
                    errorText = goalError,
                    initialMetric = currentMetricType,
                    onTextChange = {
                        goalInput = it
                        goalError = null
                    },
                    onDismiss = { showGoalSheet = false },
                    onSave = { selectedMetric ->
                        val value = goalInput.trim()
                        val parsed = value.toIntOrNull()
                        when {
                            value.isEmpty() -> goalError = "값을 입력하세요."
                            parsed == null || parsed <= 0 -> goalError = "양의 정수를 입력하세요."
                            parsed < 1000 -> goalError = "최소 1,000 이상을 권장해요."
                            else -> {
                                totalGroupGoal = parsed
                                currentMetricType = selectedMetric
                                onSaveGoal(parsed) // 실제 저장 콜백 (API)
                                showGoalSheet = false
                                scope.launch { snackbar.showSnackbar("목표가 저장되었어요.") }
                            }
                        }
                    }
                )
            }
        } // Box
    } // Scaffold
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

// 메트릭 타입 enum
enum class MetricType(
    val displayName: String,
    val unit: String,
    val icon: String,
    val placeholder: String
) {
    STEPS("걸음수", "걸음", "👣", "20000"),
    CALORIES("칼로리", "kcal", "🔥", "200"),
    EXERCISE_TIME("운동시간", "분", "⏱️", "20"),
    DISTANCE("운동거리", "km", "🏃", "5")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditSheet(
    initialText: String,
    errorText: String?,
    initialMetric: MetricType,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MetricType) -> Unit
) {
    var selectedMetric by remember { mutableStateOf(initialMetric) }

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
            // 제목 + 새로고침 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("그룹 목표 설정", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = black)
                IconButton(onClick = {
                    // TODO: 새로고침 로직 추가 여기에
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = main)
                }
            }
            Spacer(Modifier.height(12.dp))

            // 메트릭 선택 버튼
            Text("측정 항목", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF666666))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricType.values().forEach { metric ->
                    MetricButton(
                        metric = metric,
                        isSelected = selectedMetric == metric,
                        onClick = { selectedMetric = metric },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // 목표 입력 필드
            OutlinedTextField(
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFCCCCCC),
                    unfocusedBorderColor = Color(0xFFCCCCCC),

                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),

                value = initialText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("목표 ${selectedMetric.displayName}") },
                suffix = { Text(selectedMetric.unit) },
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
                    onClick = { onSave(selectedMetric) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = main, contentColor = white),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("저장") }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetricButton(
    metric: MetricType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) main else Color.White,
            contentColor = if (isSelected) white else Color(0xFF666666)
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(metric.icon, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                metric.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
