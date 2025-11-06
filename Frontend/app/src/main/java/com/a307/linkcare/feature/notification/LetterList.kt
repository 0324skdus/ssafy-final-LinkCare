package com.a307.linkcare.feature.notification

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R // << 여기!
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.util.painterResourceCropped
import java.text.SimpleDateFormat
import java.util.Locale

enum class GroupType { CARE, HEALTH }

data class LetterItem(
    val id: String,
    val title: String,
    val sender: String,
    val content: String,
    val group: GroupType,
    val sentAtMillis: Long,
    @DrawableRes val avatarRes: Int,
    val isRead: Boolean = false
)

@Composable
fun LetterList() {
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }
    val readGray = Color(0xFF9E9E9E)

    // 샘플 데이터
    val letters = remember {
        mutableStateListOf(
            LetterItem(
                id = "1",
                title = "라연언전가",
                sender = "장혜예",
                content = "너가 요즘 블루베리도 먹고...",
                group = GroupType.CARE,
                sentAtMillis = System.currentTimeMillis() - 5 * 60 * 1000L,
                avatarRes = R.drawable.char_bear_1
            ),
            LetterItem(
                id = "2",
                title = "곡연안전가",
                sender = "장혜예",
                content = "너가 요즘 블루베리도 먹고...",
                group = GroupType.HEALTH,
                sentAtMillis = System.currentTimeMillis() - 60 * 60 * 1000L,
                avatarRes = R.drawable.char_bear_2
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
            items(items = letters, key = { it.id }) { letter ->
                val label = if (letter.group == GroupType.CARE) "[케어]" else "[헬스]"
                val textColor = if (letter.isRead) readGray else black
                val subTextColor = if (letter.isRead) readGray else black

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clickable {
                            // 항목 탭 → 읽음 처리
                            val idx = letters.indexOfFirst { it.id == letter.id }
                            if (idx != -1 && !letters[idx].isRead) {
                                letters[idx] = letters[idx].copy(isRead = true)
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
                        // 1) 편지 아이콘 → 보낸 사람 캐릭터 이미지
                        Image(
                            painter = painterResourceCropped(letter.avatarRes),
                            contentDescription = "${letter.sender} 캐릭터",
                            modifier = Modifier
                                .size(37.dp)
                                .clip(CircleShape)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // 상단: [그룹] 제목 - 보낸이
                            Text(
                                text = "$label ${letter.title} - ${letter.sender}",
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            // 본문 (두 줄)
                            Text(
                                text = letter.content,
                                maxLines = 2,
                                color = subTextColor
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // 2) 우측 끝 날짜
                        Text(
                            text = formatter.format(letter.sentAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.4f))
                }
            }
        }

        // 3) 모두 읽음 버튼
        Button(
            onClick = {
                for (i in letters.indices) {
                    if (!letters[i].isRead) letters[i] = letters[i].copy(isRead = true)
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
