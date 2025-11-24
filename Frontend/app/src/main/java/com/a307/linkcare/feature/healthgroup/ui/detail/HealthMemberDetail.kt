package com.a307.linkcare.feature.healthgroup.ui.detail

import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.a307.linkcare.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.CropTransparentTransformation
import com.a307.linkcare.common.util.painterResourceCropped
import com.a307.linkcare.feature.ai.ui.AiSuggestionViewModel
import com.a307.linkcare.common.network.TokenStore
import com.a307.linkcare.feature.caregroup.ui.detail.HealthToday
import com.a307.linkcare.common.component.page.NudgeLetterCard
import com.a307.linkcare.common.component.page.Suggestion
import com.a307.linkcare.common.component.page.User
import com.a307.linkcare.feature.commongroup.domain.model.Member
import com.a307.linkcare.feature.mypage.data.model.response.GroupCharacterResponse
import com.a307.linkcare.feature.healthgroup.data.model.request.ActualActivity
import kotlin.math.roundToInt

@Composable
fun HealthMemberDetail(
    member: Member,
    avatarUrl: String?,
    backgroundUrl: String?,
    petName: String?,
    myCharInfo: GroupCharacterResponse?,
    today: HealthToday,
    dailyActivity: ActualActivity?,
    groupSeq: Long,
    onDismiss: () -> Unit,
    currentUserName: String = "나",
    maxWidth: Dp = 380.dp,
    isLoading: Boolean = false
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSelf = remember(member.userPk, TokenStore(context).getUserPk()) {
        member.userPk == TokenStore(context).getUserPk()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
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
                                        .size(Size.ORIGINAL)
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

                // 건강 정보
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "건강 데이터 로딩 중...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    } else {
                        // 일일 활동 (ActualActivity 사용)
                        val totalCalories = dailyActivity?.totalCalories?.toInt() ?: 0
                        val totalDistance = dailyActivity?.totalDistances ?: 0.0
                        val totalDuration = dailyActivity?.totalDuration ?: 0
                        val totalDurationMin = (totalDuration / 60000).toInt()  // 밀리초 → 분
                        val steps = dailyActivity?.totalSteps ?: 0

                        val summary = if (dailyActivity == null) {
                            "오늘 운동 기록이 없습니다"
                        } else if (totalCalories > 0 || totalDuration > 0) {
                            "${totalCalories} kcal 소모 (${totalDurationMin}분)"
                        } else {
                            "$steps 걸음"
                        }

                        val status = if (totalDurationMin >= 30) "양호" else if (totalDurationMin >= 15) "보통" else null

                        HealthAccordion(
                            icon = "🏃",
                            title = "일일 활동",
                            summary = summary,
                            status = status
                        ) {
                            if (dailyActivity == null || (totalCalories == 0 && totalDuration == 0 && steps == 0)) {
                                EmptyDataText()
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (totalCalories > 0) {
                                        DetailRow(
                                            "총 칼로리 소모",
                                            "$totalCalories kcal",
                                            bold = true
                                        )
                                    }
                                    if (totalDuration > 0) {
                                        DetailRow(
                                            "총 운동 시간",
                                            "${totalDurationMin}분",
                                            bold = true
                                        )
                                    }
                                    if (totalDistance > 0) {
                                        DetailRow(
                                            "총 거리",
                                            "${"%.2f".format(totalDistance / 1000)} km",
                                            bold = true
                                        )
                                    }
                                    if (steps > 0) {
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

                    Spacer(Modifier.height(8.dp))
                }

                var showLetter by remember { mutableStateOf(false) }
                var loadRequested by remember { mutableStateOf(false) }

                // ④ AI 제안 로드 (wellness-other)
                val suggestionViewModel: AiSuggestionViewModel = hiltViewModel()
                val aiSuggestions by suggestionViewModel.healthGroupSuggestions.collectAsState()
                val isAiLoading by suggestionViewModel.isLoading.collectAsState()

                // 로딩 완료 후 다이얼로그 열기 (AI 실패해도 열림)
                LaunchedEffect(loadRequested, isAiLoading) {
                    if (loadRequested && !isAiLoading) {
                        showLetter = true
                        loadRequested = false
                    }
                }

                // 편지쓰기 버튼
                Button(
                    enabled = !isSelf && !isAiLoading,
                    onClick = {
                        // 캐시가 있으면 즉시 다이얼로그 열기
                        if (suggestionViewModel.hasHealthGroupCache(member.userPk)) {
                            showLetter = true
                        } else {
                            // 캐시 없으면 로드 시작 (LaunchedEffect가 완료 감지)
                            loadRequested = true
                            suggestionViewModel.loadHealthGroupSuggestions(member.userPk, groupSeq)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = main)
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = white,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "💌 편지쓰기",
                            color = white,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
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

                    Dialog(onDismissRequest = {
                        showLetter = false
                    }) {
                        // AI 생성 제안 또는 기본 제안 사용
                        val suggestions = remember(aiSuggestions) {
                            if (aiSuggestions.isNotEmpty()) {
                                aiSuggestions.mapIndexed { index, text ->
                                    Suggestion(index + 1, text)
                                }
                            } else {
                                listOf(
                                    Suggestion(1, "오늘 컨디션 좋아 보여요!"),
                                    Suggestion(2, "물 자주 마셔요 💧"),
                                    Suggestion(3, "이따 운동하러 같이 가요 🏃‍♀️")
                                )
                            }
                        }

                        NudgeLetterCard(
                            sender = senderUser,
                            receiver = receiverUser,
                            suggestions = suggestions,
                            onClose = {
                                showLetter = false
                            },
                            onSend = { suggestion, custom ->
                                val content = if (suggestion != null) {
                                    suggestion.text
                                } else {
                                    custom
                                }

                                if (content.isNotBlank()) {
                                    // 편지 전송 API 호출
                                    scope.launch {
                                        val result = suggestionViewModel.sendLetterNotification(
                                            receiverUserPk = member.userPk,
                                            groupSeq = groupSeq,
                                            content = content
                                        )
                                        result.onSuccess {
                                            Toast.makeText(context, "편지를 보냈어요!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "편지 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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
    var expanded by remember { mutableStateOf(true) }
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

/* ---------- 유틸 함수 ---------- */
private fun formatDuration(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "${min}분 ${sec}초"
}

private fun formatTotalDuration(seconds: Long): String {
    val hours = seconds / 3600
    val min = (seconds % 3600) / 60
    return if (hours > 0) "${hours}시간 ${min}분" else "${min}분"
}

private fun Float.round1() = (this * 10).roundToInt() / 10f