package com.a307.linkcare.feature.mypage.ui.mygroups

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.feature.commongroup.ui.home.MyGroupsViewModel
import com.a307.linkcare.feature.commongroup.data.model.response.MyGroupResponse
import com.a307.linkcare.feature.mypage.data.model.dto.GroupSummary

enum class GroupType { CARE, HEALTH }

fun MyGroupResponse.toGroupSummary(): GroupSummary {
    return GroupSummary(
        id = groupSeq,
        name = groupName,
        type = if (type == "HEALTH") GroupType.HEALTH else GroupType.CARE,
        minGoalText = "",
        desc = groupDescription,
        current = currentMembers,
        max = capacity,
        imageUrl = imageUrl,
        joinStatus = joinStatus,
        isPending = joinStatus == "PENDING"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroups (
    navController: NavController,
    modifier: Modifier = Modifier,
    onGroupClick: (GroupSummary) -> Unit = {}
) {
    val viewModel: MyGroupsViewModel = hiltViewModel()

    val careGroups by viewModel.careGroups.collectAsState()
    val healthGroups by viewModel.healthGroups.collectAsState()
    val carePendingGroups by viewModel.carePendingGroups.collectAsState()
    val healthPendingGroups by viewModel.healthPendingGroups.collectAsState()

    // 주기적 자동 새로고침 (10초마다)
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10초마다
            Log.d("MyGroups", "주기적 자동 새로고침 시작")
            viewModel.loadCare()
            viewModel.loadHealth()
            viewModel.loadCarePending()
            viewModel.loadHealthPending()
        }
    }

    // 데이터 로드 - 화면이 표시될 때마다 새로고침
    LaunchedEffect(key1 = navController.currentBackStackEntry) {
        Log.d("MyGroups", "화면 진입 - 데이터 새로고침 시작")
        viewModel.loadCare()
        viewModel.loadHealth()
        viewModel.loadCarePending()
        viewModel.loadHealthPending()
    }

    // SharedFlow 이벤트 구독 - 그룹 신청 시 실시간 업데이트
    LaunchedEffect(Unit) {
        viewModel.refreshEvent.collect {
            Log.d("MyGroups", "그룹 신청 이벤트 수신 - 목록 새로고침")
            viewModel.loadCare()
            viewModel.loadHealth()
            viewModel.loadCarePending()
            viewModel.loadHealthPending()
        }
    }

    // MEMBER 상태 그룹 (내 그룹)
    val myGroups = remember(careGroups, healthGroups) {
        val groups = (careGroups + healthGroups).map { it.toGroupSummary() }
        Log.d("MyGroups", "내 그룹: ${groups.size}개")
        groups
    }

    // PENDING 상태 그룹 (신청 목록)
    val pendingGroups = remember(carePendingGroups, healthPendingGroups) {
        val groups = (carePendingGroups + healthPendingGroups).map { it.toGroupSummary() }
        Log.d("MyGroups", "신청 목록: ${groups.size}개")
        groups
    }

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("내 그룹", "신청 목록")

    Scaffold(
    ) { _ ->
        Column(
            modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                contentColor = main,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[tabIndex])
                            .height(3.dp),
                        color = main
                    )
                }
            ) {
                tabs.forEachIndexed { i, label ->
                    Tab(
                        selected = tabIndex == i,
                        onClick = { tabIndex = i },
                        selectedContentColor = main,
                        unselectedContentColor = Color(0xFF8E8E93),
                        text = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // List
            val list = if (tabIndex == 0) myGroups else pendingGroups
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { g ->
                    GroupCard(
                        group = g,
                        onClick = { onGroupClick(g) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: GroupSummary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = white,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = DividerDefaults.color.let { BorderStroke(1.dp, it.copy(alpha = 0.4f)) }
    ) {
        Column(Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 그룹 이미지
                if (!group.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = group.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(10.dp))

                // [헬스] [케어] 라벨 + 그룹명
                Text(
                    text = "[${if (group.type == GroupType.HEALTH) "헬스" else "케어"}] ${group.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 대기중 버튼 (신청 목록일 때만)
                if (group.isPending) {
                    AssistChip(
                        onClick = {},
                        label = { Text("대기중", fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFF2F2F7),
                            labelColor = Color(0xFF6D6D72)
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = group.minGoalText,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = group.desc,
                fontSize = 13.sp,
                color = Color(0xFF3A3A3A),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // 현재 / 최대 인원
            Text(
                text = "현재 인원 ${group.current} / ${group.max} 명",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/* ---------------- 샘플 데이터 & 미리보기 ---------------- */

private fun sampleMyGroups() = listOf(
    GroupSummary(
        id = 1L,
        name = "라면인건가",
        type = GroupType.HEALTH,
        minGoalText = "300kcal | 30min | 4000steps | 3km",
        desc = "우리는 하루 1000칼로리를 태우는 것을 목표로 하루 운동 3시간, 수영 2시간, 기타 운동을 하는 사람을 모집합니다.",
        current = 3, max = 6,
        imageRes = R.drawable.ic_launcher_foreground
    ),
    GroupSummary(
        id = 2L,
        name = "라면인건가",
        type = GroupType.CARE,
        minGoalText = "300kcal | 30min | 4000steps | 3km",
        desc = "우리는 하루 1000칼로리를 태우는 것을 목표로 하루 운동 3시간, 수영 2시간, 기타 운동을 하는 사람을 모집합니다.",
        current = 3, max = 6,
        imageRes = R.drawable.ic_launcher_foreground
    )
)

private fun samplePendingGroups() = listOf(
    sampleMyGroups().first().copy(id = 100, isPending = true),
    sampleMyGroups().last().copy(id = 101, isPending = true)
)