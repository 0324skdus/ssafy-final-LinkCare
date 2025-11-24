package com.a307.linkcare.common.component.molecules.memberrow

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.component.atoms.Avatar
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.gray
import com.a307.linkcare.common.theme.point
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.feature.commongroup.domain.model.Member

@Composable
fun MemberRowCare(
    m: Member,
    avatarUrl: String?,
    backgroundUrl: String?,
    petName: String?,
    bubbleVisible: Boolean,
    onAvatarToggle: () -> Unit,
    onClick: (() -> Unit)? = null,
    isSelf: Boolean = false,
    onTapIconClick: (() -> Unit)? = null,
    isExercising: Boolean = false
) {
    var showBubble by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타
        Avatar(
            avatarUrl = avatarUrl,
            backgroundUrl = backgroundUrl,
            petName = petName,
            bubbleText = m.bubbleText,
            bubbleVisible = showBubble,
            onBubbleVisibleChange = { showBubble = it },
            isExercising = isExercising
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (petName != null) {
                    Text(petName, fontWeight = FontWeight.SemiBold, color = black)
                }
                else
                    Text(m.name, fontWeight = FontWeight.SemiBold, color = black)


                Spacer(Modifier.width(6.dp))

                if (isSelf) {
                    Surface(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "나",
                            color = white,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                if (m.isLeader) {
                    Surface(
                        color = main,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "그룹장",
                            color = white,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 각자 bar 필요하면 주석해제
//            LinearProgressIndicator(
//                progress = m.percent,
//                modifier = Modifier
//                    .fillMaxWidth(0.7f)
//                    .height(6.dp)
//                    .clip(RoundedCornerShape(8.dp)),
//                color = main,
//                trackColor = Color(0x22000000)
//            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            // 상태(status) → 텍스트 컬러는 상태별로 구분 가능
            when (m.status) {
                "PERFECT" -> "완벽"
                "GOOD" -> "양호"
                "CAUTION" -> "주의"
                "ANALYZING" -> "분석 중"
                "UNKNOWN" -> "알 수 없음"
                else -> m.status
            }?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold,
                    color = when (m.status) {
                        "PERFECT" -> point
                        "GOOD" -> main
                        "CAUTION" -> Color(0xFFFFA000)
                        "ANALYZING" -> Color(0xFF888888)
                        "UNKNOWN" -> Color(0xFF888888)
                        else -> main
                    }
                )
            }

            Spacer(Modifier.height(2.dp))

            // 요약(summary)
            m.summary?.let {
                val formattedSummary = it.replace(". ", ".\n")
                Text(
                    text = formattedSummary,
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    maxLines = 3,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.End
                )
            }
        }
        if (!isSelf) {
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.tap),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .then(if (onTapIconClick != null) Modifier.clickable { onTapIconClick() } else Modifier)
            )
        } else {
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.tap),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(gray)
            )
        }
    }
}
