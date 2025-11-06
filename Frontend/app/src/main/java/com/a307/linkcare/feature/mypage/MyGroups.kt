package com.a307.linkcare.feature.mypage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.a307.linkcare.R
import com.a307.linkcare.common.component.molecules.HeaderBack
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.unActiveField
import com.a307.linkcare.common.theme.white

enum class GroupType { CARE, HEALTH }

data class GroupSummary(
    val id: Long,
    val name: String,
    val type: GroupType,
    val minGoalText: String,          // 예: "300kcal | 30min | 4000steps | 3km"
    val desc: String,
    val current: Int,
    val max: Int,
    @DrawableRes val imageRes: Int,   // 네가 줄 사진(res/drawable)로 교체
    val isPending: Boolean = false    // 신청 목록이면 true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroups (
    navController: NavController,
    modifier: Modifier = Modifier,
    myGroups: List<GroupSummary> = sampleMyGroups(),
    pendingGroups: List<GroupSummary> = samplePendingGroups(),
    onGroupClick: (GroupSummary) -> Unit = {}
) {
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
                Image(
                    painter = painterResource(group.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

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