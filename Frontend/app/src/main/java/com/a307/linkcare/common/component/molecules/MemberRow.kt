package com.a307.linkcare.common.component.molecules

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.component.atoms.Avatar
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.point
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.feature.commongroup.domain.Member

@Composable
fun MemberRow(
    m: Member,
    bubbleVisible: Boolean,
    onAvatarToggle: () -> Unit,
    onClick: (() -> Unit)? = null
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
            imageRes = m.avatarRes,
            bubbleText = m.bubbleText,
            bubbleVisible = showBubble,
            onBubbleVisibleChange = { showBubble = it },
            bgImgRes = R.drawable.background_1
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, fontWeight = FontWeight.SemiBold, color = black)
                if (m.isLeader) {
                    Spacer(Modifier.width(6.dp))
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
        Column(horizontalAlignment = Alignment.End) {
            Text("${m.progresses} 달성", fontWeight = FontWeight.Bold, color = if (m.goal > m.progresses) main else point)
            Text("목표 ${m.goal}", fontSize = 12.sp, color = Color(0xFF888888))
        }
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.tap),
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )
    }
}
