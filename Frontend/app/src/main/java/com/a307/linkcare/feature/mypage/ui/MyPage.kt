@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.CropTransparentTransformation
import com.a307.linkcare.feature.auth.data.RetrofitClient
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.caregroup.ActivitySessionCard
import com.a307.linkcare.feature.caregroup.DetailRow
import com.a307.linkcare.feature.caregroup.EmptyDataText
import com.a307.linkcare.feature.caregroup.HealthStatus
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.formatTotalDuration
import com.a307.linkcare.feature.caregroup.getActivityStatus
import com.a307.linkcare.feature.caregroup.getActivitySummary
import com.a307.linkcare.feature.caregroup.getBloodPressureStatus
import com.a307.linkcare.feature.caregroup.getBloodPressureSummary
import com.a307.linkcare.feature.caregroup.getSleepStatus
import com.a307.linkcare.feature.caregroup.getSleepSummary
import com.a307.linkcare.feature.caregroup.getTotalSleepHours
import com.a307.linkcare.feature.caregroup.getWaterStatus
import com.a307.linkcare.feature.caregroup.getWaterSummary
import com.a307.linkcare.feature.caregroup.round1
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

private val shadowColor = Color(0x1A000000)


data class MyPageUiState(
    val nickname: String,
    val coinLabel: String,
    val groupCountLabel: String,
    val avatarUrl: String?,
    val avatarBgUrl: String? = null,
    val healthToday: HealthToday? = null
)

@Composable
fun MyPage(
    state: MyPageUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onDecorateClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val performLogout: () -> Unit = {
        scope.launch {
            try {
                val access = TokenStore(context).getAccess()
                if (access == null) {
                    Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val res = RetrofitClient.authApi.logout("Bearer $access")

                if (res.isSuccessful) {
                    TokenStore(context).clear()
                    Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

                    onLogout()
                } else {
                    Toast.makeText(context, "로그아웃 실패", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "서버 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        onRefresh = onRefresh
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(white)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // 프로필 카드
                ProfileCard(
                    state = state,
                    onEditClick = onEditClick,
                    onLogoutClick = performLogout,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // 소속 그룹 버튼
                Surface(
                    onClick = onGroupsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = main.copy(alpha = 0.25f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = main
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        main,
                                        main.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.groupCountLabel,
                            color = white,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 하단 카드 2열
                MenuRow(
                    state = state,
                    onStoreClick = onStoreClick,
                    onDecorateClick = onDecorateClick
                )

                Spacer(Modifier.height(24.dp))

                // 오늘의 건강 정보
                Text(
                    "오늘 나의 건강 정보",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(12.dp))

                if (state.healthToday != null) {
                    HealthInfoSection(today = state.healthToday)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }


                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/* ---------- 내부 구성요소 ---------- */
@Composable
private fun ProfileCard(
    state: MyPageUiState,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val imageSize = with(density) { 120.dp.toPx().toInt() }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = shadowColor
            ),
        color = white,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                // 상단 버튼 영역
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onLogoutClick,
                        modifier = Modifier.height(24.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "로그아웃",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }

                // 본문 영역
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 아바타 + 배경
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 배경 원형 이미지
                        AsyncImage(
                            model = state.avatarBgUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // 아바타 이미지
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(state.avatarUrl)
                                .crossfade(true)
                                .size(imageSize)
                                .transformations(CropTransparentTransformation())
                                .build(),
                            contentDescription = "avatar",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )

                        // 편집 버튼 (우측 하단, 배경에 살짝 걸치게)
                        Surface(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .align(Alignment.BottomEnd),
                            shape = CircleShape,
                            color = white,
                            shadowElevation = 4.dp,
                            tonalElevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "편집",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF666666)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.nickname,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = black
                        )
                        Spacer(Modifier.height(8.dp))

                        // 코인 표시
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.coin),
                                contentDescription = "coin",
                                modifier = Modifier.size(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                state.coinLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    state: MyPageUiState,
    onStoreClick: () -> Unit,
    onDecorateClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        MenuButtonCard(
            state = state,
            title = "상점",
            icon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(24.dp)) },
            modifier = Modifier.weight(1f),
            onClick = onStoreClick,
            iconColor = Color(0xFF4CAF50)
        )
        Spacer(Modifier.width(16.dp))
        MenuButtonCard(
            state = state,
            title = "보관함",
            icon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(24.dp)) },
            modifier = Modifier.weight(1f),
            onClick = onDecorateClick,
            iconColor = Color(0xFFFF9800)
        )
    }
}

@Composable
private fun MenuButtonCard(
    state: MyPageUiState,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    iconColor: Color = main,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = shadowColor
            ),
        color = white,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }

            Spacer(Modifier.width(14.dp))
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = black
            )
        }
    }
}

@Composable
private fun HealthInfoSection(today: HealthToday) {
    Column(
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
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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
}

@Composable
private fun HealthAccordion(
    icon: String,
    title: String,
    summary: String,
    status: HealthStatus = HealthStatus.Neutral,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = white,
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    if (summary.isNotEmpty()) {
                        Text(summary, fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "축소" else "확장",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    tint = Color.Gray
                )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFF0F0F0)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: HealthStatus) {
    val (text, color) = when (status) {
        HealthStatus.Good -> "좋음" to Color(0xFF34C759)
        HealthStatus.Warning -> "주의" to Color(0xFFFF9500)
        HealthStatus.Bad -> "나쁨" to Color(0xFFFF3B30)
        HealthStatus.Neutral -> "" to Color.Transparent
    }

    if (text.isNotEmpty()) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
        Spacer(Modifier.width(8.dp))
    }
}
