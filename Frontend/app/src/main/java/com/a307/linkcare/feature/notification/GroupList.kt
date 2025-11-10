package com.a307.linkcare.feature.notification

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.util.painterResourceCropped
import java.text.SimpleDateFormat
import java.util.Locale

private val ReadGray = Color(0xFF9E9E9E)
enum class JoinAlertKind { JOIN_REQUEST, JOIN_ACCEPTED, JOIN_REJECTED }

data class GroupJoinAlert(
    val id: String,
    val groupName: String,
    val groupType: GroupType,
    val kind: JoinAlertKind,
    val sentAtMillis: Long,
    val senderName: String,
    @DrawableRes val senderAvatarRes: Int?,
    @DrawableRes val groupImageRes: Int,
    val isRead: Boolean = false
)

@Composable
fun GroupList() {
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }

    val alerts = remember {
        mutableStateListOf(
            GroupJoinAlert(
                id = "1",
                groupName = "라연언전가",
                groupType = GroupType.CARE,
                kind = JoinAlertKind.JOIN_REQUEST,
                sentAtMillis = System.currentTimeMillis() - 5 * 60 * 1000L,
                senderName = "김도훈",
                senderAvatarRes = R.drawable.char_bear_1,
                groupImageRes = R.drawable.ic_launcher_foreground
            ),
            GroupJoinAlert(
                id = "2",
                groupName = "곡연안전가",
                groupType = GroupType.HEALTH,
                kind = JoinAlertKind.JOIN_ACCEPTED,
                sentAtMillis = System.currentTimeMillis() - 60 * 60 * 1000L,
                senderName = "김도훈",
                senderAvatarRes = null,
                groupImageRes = R.drawable.ic_launcher_foreground
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(items = alerts, key = { it.id }) { alert ->
                val label = if (alert.groupType == GroupType.CARE) "[케어]" else "[헬스]"
                val textColor = if (alert.isRead) ReadGray else black

                // 문구 3종
                val message = when (alert.kind) {
                    JoinAlertKind.JOIN_REQUEST -> "${alert.senderName}님이 그룹 가입을 신청했습니다."
                    JoinAlertKind.JOIN_ACCEPTED -> "${alert.senderName}님의 그룹 가입을 수락했습니다."
                    JoinAlertKind.JOIN_REJECTED -> "${alert.senderName}님의 그룹 가입을 거절했습니다."
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clickable {
                            val idx = alerts.indexOfFirst { it.id == alert.id }
                            if (idx != -1 && !alerts[idx].isRead) {
                                alerts[idx] = alerts[idx].copy(isRead = true)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = white)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val thumbRes = alert.senderAvatarRes ?: alert.groupImageRes

                        Image(
                            painter = painterResourceCropped(thumbRes),
                            contentDescription = null,
                            modifier = Modifier
                                .size(37.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$label ${alert.groupName}",
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = formatter.format(alert.sentAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }

                    // 요청일 때만 하단 버튼 (LetterList 기준 spacing)
                    if (alert.kind == JoinAlertKind.JOIN_REQUEST) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val idx = alerts.indexOfFirst { it.id == alert.id }
                                    if (idx != -1) {
                                        alerts[idx] = alerts[idx].copy(
                                            kind = JoinAlertKind.JOIN_REJECTED,
                                            isRead = true
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, Color(0x33FF6B35)),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("거절", color = Color(0xFFFF6B35)) }

                            Spacer(Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val idx = alerts.indexOfFirst { it.id == alert.id }
                                    if (idx != -1) {
                                        alerts[idx] = alerts[idx].copy(
                                            kind = JoinAlertKind.JOIN_ACCEPTED,
                                            isRead = true
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, Color(0x338ECFA9)),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("수락", color = Color(0xFF8ECFA9)) }
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.4f))
                }
            }
        }

        // 모두 읽음(회색 처리)
        if (alerts.any { !it.isRead }) {
            Button(
                onClick = {
                    for (i in alerts.indices) {
                        if (!alerts[i].isRead) alerts[i] = alerts[i].copy(isRead = true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = main,
                    contentColor = white
                )
            ) { Text("모두 읽음") }
        }
    }
}
