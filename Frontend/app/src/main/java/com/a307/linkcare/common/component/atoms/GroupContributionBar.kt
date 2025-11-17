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
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.feature.commongroup.domain.Member

@Composable
fun GroupContributionBar(
    members: List<Member>,
    modifier: Modifier = Modifier
) {
    val summaries = toSummaries(members)

    val goal: Int = members.firstOrNull()?.goal
        ?.takeIf { it > 0 }
        ?: members.sumOf { it.progresses }
            .takeIf { it > 0 }
        ?: 1 // 0 나눗셈 방지용

    // 각 멤버의 기여도 (0.0 ~ 1.0)
    data class Segment(val fraction: Float, val color: Color, val label: String)

    val segments: List<Segment> = members.mapIndexed { index, m ->
        val fraction = (m.progresses.toFloat() / goal.toFloat())
            .coerceIn(0f, 1f) // 이상값 방지

        Segment(
            fraction = fraction,
            color = summaries.getOrNull(index)?.color ?: Color.Gray,
            label = summaries.getOrNull(index)?.label ?: m.name
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // --- 상단 전체 기여도 Progress Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E9F0))
        ) {
            // 배경 위에 색깔 구간 Row로 쌓기
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                segments.forEach { s ->
                    if (s.fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(s.fraction) // 기여도 비율만큼 폭 차지
                                .background(s.color)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 하단 범례 ---
        Column {
            segments.forEach { s ->
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
