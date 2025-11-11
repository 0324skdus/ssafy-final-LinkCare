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
    val totalProgress = summaries.sumOf { it.progress.toDouble() }.toFloat().coerceAtMost(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // --- 상단 전체 합산 Progress Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E9F0))
        ) {
            // 각 멤버 기여 구간 나누어 색 표시
            var startFraction = 0f
            summaries.forEach { s ->
                val widthFraction = s.progress / totalProgress
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(widthFraction)
                        .align(Alignment.CenterStart)
                        .offset(x = (startFraction * 100).dp / 100)
                        .background(s.color)
                )
                startFraction += widthFraction
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
