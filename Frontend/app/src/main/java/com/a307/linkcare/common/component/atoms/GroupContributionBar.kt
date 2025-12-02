package com.a307.linkcare.common.component.atoms


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import com.a307.linkcare.common.component.molecules.health.toSummaries
import com.a307.linkcare.feature.commongroup.domain.model.Member

@Composable
fun GroupContributionBar(
    members: List<Member>,
    modifier: Modifier = Modifier
) {
    // 목표(goal)는 모든 멤버에게 동일하게 '그룹 전체 목표'로 설정되어 있다고 가정
    val totalGoal = members.firstOrNull()?.goal?.toFloat() ?: 1f
    if (totalGoal <= 0f) {
        // 목표가 0이거나 설정되지 않은 경우, 빈 막대만 표시
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E9F0))
        )
        return
    }

    // 모든 멤버가 달성한 총량
    val totalAchieved = members.sumOf { it.progresses }.toFloat()
    // 목표까지 남은 양 (음수 방지)
    val emptyProgress = (totalGoal - totalAchieved).coerceAtLeast(0f)

    val summaries = toSummaries(members) // 범례 표시에 필요

    Column(modifier = modifier.fillMaxWidth()) {
        // --- 상단 전체 합산 Progress Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // 각 멤버의 기여도를 weight로 그림
            summaries.forEachIndexed { index, summary ->
                val member = members[index]
                if (member.progresses > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(member.progresses.toFloat())
                            .background(summary.color)
                    )
                }
            }
            // 목표까지 남은 빈 공간을 그림
            if (emptyProgress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(emptyProgress)
                        .background(Color(0xFFE5E9F0))
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 하단 범례 ---
        Column {
            summaries.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(s.color, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
