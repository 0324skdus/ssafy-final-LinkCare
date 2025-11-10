package com.a307.linkcare.feature.commongroup

import com.a307.linkcare.feature.commongroup.domain.GroupItem
import com.a307.linkcare.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white


private val dummyGroups = listOf(
    GroupItem(
        id = 1,
        title = "라면안먹기 (3/6)",
        summary = "일주일에 10000보를 달리는 건강 목표! 수요일 3회, 수면 7시간, 꾸준 24주, 개인 활동을 하는 소셜형 그룹입니다.",
        pace = "300kcal | 30min | 4000steps | 3km",
        members = "3/6",
        thumbnailRes = null // 썸네일 없으면 아이콘 대체
    ),
    GroupItem(
        id = 2,
        title = "러닝런닝가 (3/6)",
        summary = "주 3회 함께 뛰는 달리기 그룹. 초보 환영!",
        pace = "250kcal | 25min | 3000steps | 2.4km",
        members = "3/6",
        thumbnailRes = null
    ),
    GroupItem(
        id = 3,
        title = "걷기먼저 (5/8)",
        summary = "매일 가볍게 걷고 수다 떠는 따뜻한 그룹.",
        pace = "200kcal | 20min | 3000steps | 2km",
        members = "5/8",
        thumbnailRes = null
    )
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSearch(
    kind: String, // "care" | "health"
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onJoinClick: (GroupItem) -> Unit
) {
    // 더미 데이터
    val careDummy = remember {
        listOf(
            GroupItem(1, "케어-식단관리", "식단 기록하고 리뷰하는 그룹", "200kcal | 20min | 3000steps | 2km", "3/6"),
            GroupItem(2, "케어-수면개선", "취침/기상 루틴 만들기", "200kcal | 20min | 3000steps | 2km", "4/6")
        )
    }
    val healthDummy = remember {
        listOf(
            GroupItem(10, "헬스-만수무강", "주 3회 러닝", "300kcal | 30min | 4000steps | 3km", "3/6"),
            GroupItem(11, "헬스-걷기먼저", "매일 가볍게 걷기", "200kcal | 20min | 3000steps | 2km", "5/8")
        )
    }

    val source = if (kind == "health") healthDummy else careDummy

    val results = remember(query, source) {
        if (query.isBlank()) emptyList()
        else source.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true)
        }
    }

    Scaffold { padding ->
        when {
            // 입력 없을 때 (아무것도 안보여줌)
            query.isBlank() -> {}

            // 결과 없음
            results.isEmpty() -> EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                message = "앗! 검색 결과가 없어요."
            )

            // 결과 있음
            else -> LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    GroupResultCard(item = item, onJoinClick = { onJoinClick(item) })
                }
            }
        }
    }
}


// --------- 카드 UI ----------
@Composable
private fun GroupResultCard(
    item: GroupItem,
    onJoinClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: 상세 진입 처리 */ },
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── 상단: 썸네일 + 제목/멤버 + 신청 버튼 ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 썸네일
                Box(modifier = Modifier.size(44.dp)) {
                    if (item.thumbnailRes != null) {
                        Image(
                            painter = painterResource(item.thumbnailRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // 제목 + 멤버칩
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = white
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = main
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = item.members,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = main
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(containerColor = main),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text("신청", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── 하단: 그룹 페이스 & 설명 (카드의 맨 아래 영역) ─────────────────────
            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF9FAFB)
            ) {
                Text(
                    text = item.pace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563),
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --------- 빈 상태 공용 컴포넌트 ----------
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.empty_duck), null, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(4.dp))
            Text(message, color = Color(0xFF6B7280))
        }
    }
}