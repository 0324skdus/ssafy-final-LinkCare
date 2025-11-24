package com.a307.linkcare.common.component.molecules.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.component.atoms.GroupContributionBar
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.feature.commongroup.domain.model.Member
import java.text.NumberFormat

// 데이터 클래스 정의
data class ProgressSummary(
    val label: String,
    val progress: Float,
    val color: Color
)

// 팔레트(멤버 수가 많아도 순환)
private val palette = listOf(
    Color(0xFF4A89F6),
    Color(0xFF66BB6A),
    Color(0xFFAB47BC),
    Color(0xFFFFA726),
    Color(0xFFEC407A),
    Color(0xFF26C6DA)
)

private fun fmt(n: Int): String = NumberFormat.getInstance().format(n)

// Member 리스트 → 카드 렌더링용 요약 리스트로 변환
fun toSummaries(members: List<Member>): List<ProgressSummary> =
    members.mapIndexed { i, m ->
        ProgressSummary(
            label = "${m.name} · ${fmt(m.goal)} 중 ${fmt(m.progresses)}",
            progress = m.percent,
            color = palette[i % palette.size]
        )
    }

@Composable
fun ProgressSummaryCard(
    title: String,
    members: List<Member>,
    modifier: Modifier = Modifier
) {
    val summaries = toSummaries(members)

    Card(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF6F7FB),
            contentColor = Color(0xFF111827)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            // 헤더
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.padding(2.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = black)
                }
                // 달성 뱃지 (필요 시 주석해제)
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(Icons.Default.Star, contentDescription = null, tint = point)
//                    Spacer(Modifier.width(4.dp))
//                    Text(
//                        text = badgeText ?: "목표 달성",
//                        color = point,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
            }

            GroupContributionBar(members)

            Spacer(Modifier.height(10.dp))
        }
    }
}