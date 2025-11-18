package com.a307.linkcare.feature.caregroup

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.CropTransparentTransformation
import com.a307.linkcare.common.util.painterResourceCropped
import com.a307.linkcare.feature.commongroup.ui.NudgeLetterCard
import com.a307.linkcare.feature.commongroup.ui.Suggestion
import com.a307.linkcare.feature.commongroup.ui.User
import com.a307.linkcare.feature.commongroup.domain.Member
import kotlin.math.roundToInt
import com.a307.linkcare.R
import com.a307.linkcare.feature.mypage.GroupCharacterResponse

/* ---------- 모델 ---------- */

data class BloodPressure(
    val bloodPressureId: Int = 0,
    val uid: String = "",
    val startTime: String = "",
    val systolic: Float = 0f,
    val diastolic: Float = 0f,
    val mean: Float = 0f,
    val pulseRate: Int = 0
)

data class WaterIntake(
    val waterIntakeId: Int = 0,
    val startTime: String = "",
    val amount: Float = 0f
)

data class Sleep(
    val sleepId: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val duration: Int = 0  // 분 단위
)

data class ExerciseSession(
    val startTime: String = "",
    val endTime: String = "",
    val distance: Float = 0f,  // meter
    val calories: Float = 0f,
    val meanPulseRate: Float = 0f,
    val duration: Long = 0  // 초 단위
)

data class DailyActivitySummary(
    val exercises: List<ExerciseSession> = emptyList(),
    val steps: Int = 0
)

data class HeartRate(
    val heartRateId: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val heartRate: Double = 0.0
)

data class HealthToday(
    val bloodPressures: List<BloodPressure> = emptyList(),
    val waterIntakes: List<WaterIntake> = emptyList(),
    val waterGoalMl: Int = 2000,
    val sleeps: List<Sleep> = emptyList(),
    val dailyActivitySummary: DailyActivitySummary? = null,
    val heartRates: List<HeartRate> = emptyList()
)

@Composable
fun CareMemberDetail(
    member: Member,
    avatarUrl: String?,
    backgroundUrl: String?,
    petName: String?,
    myCharInfo: GroupCharacterResponse?,
    today: HealthToday,
    onDismiss: () -> Unit,
    currentUserName: String = "나",
    maxWidth: Dp = 380.dp
) {
    val isSelf = remember(member.name, currentUserName) {
        member.name == currentUserName
    }

    val context = LocalContext.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                Modifier
                    .widthIn(max = maxWidth)
                    .background(white)
            ) {
                // 헤더
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(main)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .transformations(CropTransparentTransformation())
                                        .size(coil.size.Size.ORIGINAL)
                                        .build(),
                                    contentDescription = petName,
                                    modifier = Modifier
                                        .size(35.dp),
                                    contentScale = ContentScale.Fit,
                                    onError = { error ->
                                        Log.e("Avatar", "image load error: $avatarUrl", error.result.throwable)
                                    }
                                )
                            } else {
                                Image(
                                    painter = painterResourceCropped(resId = member.avatarRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(35.dp)
                                )
                            }

                            Column {
                                if (petName != null) {
                                    Text(
                                        petName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = white,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "${member.name}님의 오늘의 건강 정보",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = white.copy(alpha = 0.9f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = white
                            )
                        }
                    }
                }

                // 건강 정보 아코디언 리스트 (스크롤 가능)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 혈압
                    HealthAccordion(
                        icon = "🫀",
                        title = "혈압",
                        summary = getBloodPressureSummary(today.bloodPressures),
                        status = getBloodPressureStatus(today.bloodPressures)
                    ) {
                        if (today.bloodPressures.isEmpty()) {
                            EmptyDataText()
                        } else {
                            today.bloodPressures.forEach { bp ->
                                val time = if (bp.startTime.contains("T")) {
                                    bp.startTime.substringAfter("T").substringBefore(".").substring(0, 5)
                                } else bp.startTime
                                DetailRow(time, "${bp.systolic.toInt()}/${bp.diastolic.toInt()} mmHg")
                            }
                        }
                    }

                    // 혈당
//                    HealthAccordion(
//                        icon = "💉",
//                        title = "혈당",
//                        summary = getGlucoseSummary(today.glucoseLogs),
//                        status = getGlucoseStatus(today.glucoseLogs)
//                    ) {
//                        if (today.glucoseLogs.isEmpty()) {
//                            EmptyDataText()
//                        } else {
//                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                                today.glucoseLogs.forEach { log ->
//                                    DetailRow(
//                                        "${log.time}${log.memo?.let { " ($it)" } ?: ""}",
//                                        "${log.mgDl} mg/dL"
//                                    )
//                                }
//                            }
//                        }
//                    }

                    // 음수량
                    HealthAccordion(
                        icon = "💧",
                        title = "음수량",
                        summary = getWaterSummary(today.waterIntakes, today.waterGoalMl),
                        status = getWaterStatus(today.waterIntakes, today.waterGoalMl)
                    ) {
                        if (today.waterIntakes.isEmpty()) {
                            EmptyDataText()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                today.waterIntakes.forEach { intake ->
                                    val time = if (intake.startTime.contains("T")) {
                                        intake.startTime.substringAfter("T").substringBefore(".").substring(0, 5)
                                    } else intake.startTime
                                    DetailRow(time, "${intake.amount.toInt()} mL")
                                }
                                Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 4.dp))
                                DetailRow(
                                    "오늘 총 섭취",
                                    "${today.waterIntakes.sumOf { it.amount.toInt() }} mL",
                                    bold = true
                                )
                                DetailRow(
                                    "하루 목표",
                                    "${today.waterGoalMl} mL",
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }

                    // 수면
                    HealthAccordion(
                        icon = "😴",
                        title = "수면",
                        summary = getSleepSummary(today.sleeps),
                        status = getSleepStatus(today.sleeps)
                    ) {
                        if (today.sleeps.isEmpty()) {
                            EmptyDataText()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                today.sleeps.forEachIndexed { idx, sleep ->
                                    val startTime = if (sleep.startTime.contains("T")) {
                                        sleep.startTime.substringAfter("T").substringBefore(".").substring(0, 5)
                                    } else sleep.startTime
                                    val endTime = if (sleep.endTime.contains("T")) {
                                        sleep.endTime.substringAfter("T").substringBefore(".").substring(0, 5)
                                    } else sleep.endTime
                                    DetailRow(
                                        if (today.sleeps.size > 1) "수면 ${idx + 1}" else "수면 시간",
                                        "$startTime ~ $endTime"
                                    )
                                }
                                Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 4.dp))
                                DetailRow(
                                    "총 수면 시간",
                                    getTotalSleepHours(today.sleeps),
                                    bold = true
                                )
                            }
                        }
                    }

                    // 일일 활동
                    val exercises = today.dailyActivitySummary?.exercises ?: emptyList()
                    HealthAccordion(
                        icon = "🏃",
                        title = "일일 활동",
                        summary = getActivitySummary(exercises, today.dailyActivitySummary?.steps ?: 0),
                        status = getActivityStatus(exercises)
                    ) {
                        if (exercises.isEmpty()) {
                            EmptyDataText()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                exercises.forEachIndexed { idx, session ->
                                    ActivitySessionCard(session, idx + 1)
                                }
                                Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DetailRow(
                                        "총 칼로리 소모",
                                        "${exercises.sumOf { it.calories.toInt() }} kcal",
                                        bold = true
                                    )
                                    DetailRow(
                                        "총 운동 시간",
                                        formatTotalDuration(exercises.sumOf { it.duration }),
                                        bold = true
                                    )
                                    DetailRow(
                                        "총 거리",
                                        "${(exercises.sumOf { it.distance.toDouble() } / 1000).toFloat().round1()} km",
                                        bold = true
                                    )
                                    today.dailyActivitySummary?.steps?.let { steps ->
                                        DetailRow(
                                            "걸음 수",
                                            "$steps 걸음",
                                            bold = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                var showLetter by remember { mutableStateOf(false) }
                // 편지쓰기 버튼
                Button(
                    enabled = !isSelf,
                    onClick = { showLetter = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = main)
                ) {
                    Text(
                        "💌 편지쓰기",
                        color = white,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (showLetter) {
                    val senderUser = User(
                        name = currentUserName,
                        avatarRes = R.drawable.char_bear_1,
                        avatarUrl = myCharInfo?.mainCharacterImageUrl,
                        backgroundUrl = myCharInfo?.mainBackgroundImageUrl,
                        petName = myCharInfo?.petName
                    )

                    val receiverUser = User(
                        name = member.name,
                        avatarRes = member.avatarRes,
                        avatarUrl = avatarUrl,
                        backgroundUrl = backgroundUrl,
                        petName = petName
                    )

                    Dialog(onDismissRequest = { showLetter = false }) {
                        NudgeLetterCard(
                            sender = senderUser,
                            receiver = receiverUser,
                            suggestions = listOf(
                                Suggestion(1, "오늘 컨디션 좋아 보여요!"),
                                Suggestion(2, "물 자주 마셔요 💧"),
                                Suggestion(3, "이따 운동하러 같이 가요 🏃‍♀️")
                            ),
                            onClose = { showLetter = false },
                            onSend = { suggestion, custom ->
                                println("보낼 내용: ${suggestion?.text ?: custom}")
                                showLetter = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ---------- 아코디언 컴포넌트 ---------- */

@Composable
private fun HealthAccordion(
    icon: String,
    title: String,
    summary: String,
    status: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 헤더
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(icon, fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = black
                            )
                            if (status != null) {
                                StatusBadge(status)
                            }
                        }
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            // 확장 컨텐츠
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Divider(color = Color(0xFFE5E7EB))
                    Surface(
                        color = white,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitySessionCard(session: ExerciseSession, index: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FA),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "운동 세션 $index",
                fontWeight = FontWeight.SemiBold,
                color = black,
                fontSize = 14.sp
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricItem("칼로리", "${session.calories.toInt()} kcal")
                    MetricItem(
                        "평균 심박수",
                        if (session.meanPulseRate > 0) "${session.meanPulseRate.toInt()} bpm" else "-"
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricItem("거리", "${(session.distance / 1000f).round1()} km")
                    MetricItem("시간", formatDuration(session.duration))
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = black
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color = black
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
private fun EmptyDataText() {
    Text(
        "오늘 데이터가 없습니다",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9CA3AF),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when {
        status.contains("정상") || status.contains("적정") || status.contains("양호") ->
            Color(0xFFDCFCE7) to Color(0xFF16A34A)
        status.contains("주의") || status.contains("부족") || status.contains("미달") ->
            Color(0xFFFEF3C7) to Color(0xFFEA580C)
        status.contains("위험") || status.contains("과다") ->
            Color(0xFFFEE2E2) to Color(0xFFDC2626)
        else -> Color(0xFFE5E7EB) to Color(0xFF6B7280)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

/* ---------- 요약 생성 함수 ---------- */

private fun getBloodPressureSummary(bps: List<BloodPressure>): String {
    if (bps.isEmpty()) return "오늘 측정 기록이 없습니다"
    val avgSys = bps.map { it.systolic }.average().roundToInt()
    val avgDia = bps.map { it.diastolic }.average().roundToInt()
    return "평균 $avgSys/$avgDia mmHg (${bps.size}회 측정)"
}

private fun getBloodPressureStatus(bps: List<BloodPressure>): String? {
    if (bps.isEmpty()) return null
    val avgSys = bps.map { it.systolic }.average().roundToInt()
    val avgDia = bps.map { it.diastolic }.average().roundToInt()
    return when {
        avgSys < 120 && avgDia < 80 -> "정상"
        avgSys < 140 && avgDia < 90 -> "주의"
        else -> "위험"
    }
}

//private fun getGlucoseSummary(logs: List<GlucoseLog>): String {
//    if (logs.isEmpty()) return "오늘 측정 기록이 없습니다"
//    val avgGlucose = logs.map { it.mgDl }.average().roundToInt()
//    return "평균 $avgGlucose mg/dL (${logs.size}회 측정)"
//}
//
//private fun getGlucoseStatus(logs: List<GlucoseLog>): String? {
//    if (logs.isEmpty()) return null
//    val avgGlucose = logs.map { it.mgDl }.average().roundToInt()
//    return when {
//        avgGlucose < 100 -> "정상"
//        avgGlucose < 126 -> "주의"
//        else -> "위험"
//    }
//}

private fun getWaterSummary(intakes: List<WaterIntake>, goalMl: Int): String {
    if (intakes.isEmpty()) return "오늘 기록이 없습니다"
    val totalMl = intakes.sumOf { it.amount.toInt() }
    val percent = ((totalMl.toFloat() / goalMl) * 100).roundToInt()
    return "${(totalMl / 1000f).round1()} L 섭취 (목표의 $percent%)"
}

private fun getWaterStatus(intakes: List<WaterIntake>, goalMl: Int): String? {
    if (intakes.isEmpty()) return null
    val totalMl = intakes.sumOf { it.amount.toInt() }
    return when {
        totalMl >= goalMl -> "양호"
        totalMl >= goalMl * 0.7f -> "적정"
        else -> "부족"
    }
}

private fun getSleepSummary(sleeps: List<Sleep>): String {
    if (sleeps.isEmpty()) return "오늘 수면 기록이 없습니다"
    val totalHours = getTotalSleepHours(sleeps)
    return "$totalHours 수면 (${sleeps.size}회)"
}

private fun getSleepStatus(sleeps: List<Sleep>): String? {
    if (sleeps.isEmpty()) return null
    val totalMillis = sleeps.sumOf { it.duration }
    val hours = totalMillis / 3600000f  // 밀리초 → 시간 (1000 * 60 * 60)
    return when {
        hours >= 7 && hours <= 9 -> "적정"
        hours < 7 -> "부족"
        else -> "과다"
    }
}

private fun getActivitySummary(exercises: List<ExerciseSession>, steps: Int): String {
    if (exercises.isEmpty() && steps == 0) return "오늘 운동 기록이 없습니다"
    val totalCalories = exercises.sumOf { it.calories.toInt() }
    val totalMin = exercises.sumOf { it.duration } / 60000  // 밀리초 → 분 (1000 * 60)
    return if (exercises.isNotEmpty()) {
        "${totalCalories} kcal 소모 (${totalMin}분)"
    } else {
        "$steps 걸음"
    }
}

private fun getActivityStatus(exercises: List<ExerciseSession>): String? {
    if (exercises.isEmpty()) return null
    val totalMin = exercises.sumOf { it.duration } / 60000  // 밀리초 → 분 (1000 * 60)
    return when {
        totalMin >= 30 -> "양호"
        totalMin >= 15 -> "보통"
        else -> "미달"
    }
}

/* ---------- 유틸 함수 ---------- */

private fun getTotalSleepHours(sleeps: List<Sleep>): String {
    val totalSeconds = sleeps.sumOf { it.duration } / 1000  // 밀리초 → 초
    val totalMinutes = totalSeconds / 60  // 초 → 분
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return "${hours}시간 ${mins}분"
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return "${min}분 ${sec}초"
}

private fun formatTotalDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val min = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}시간 ${min}분" else "${min}분"
}

private fun Float.round1() = (this * 10).roundToInt() / 10f