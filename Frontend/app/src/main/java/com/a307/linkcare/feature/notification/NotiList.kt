package com.a307.linkcare.feature.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import java.text.SimpleDateFormat
import java.util.Locale

private data class Nudge(
    val id: String,
    val text: String,
    val sentAtMillis: Long,
    val isRead: Boolean = false
)

@Composable
fun NotiList() {
    // 예: "11.06 14:35"
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }
    val readGray = Color(0xFF9E9E9E)

    // 샘플 데이터 (실제론 서버/DB 데이터 넣으면 됨)
    val nudges = remember {
        mutableStateListOf(
            Nudge("1", "정혜예: 파이팅!", System.currentTimeMillis() - 5 * 60 * 1000L),
            Nudge("2", "김혜윤: 오늘도 힘내!", System.currentTimeMillis() - 60 * 60 * 1000L),
            Nudge("3", "진혜예: 지금 뭐하고 있어?", System.currentTimeMillis() - 24 * 60 * 60 * 1000L + 10 * 60 * 1000L)
        )
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(items = nudges, key = { it.id }) { nudge ->
                val textColor = if (nudge.isRead) readGray else black

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable {
                            // 3) 항목 터치 시 읽음 처리
                            val idx = nudges.indexOfFirst { it.id == nudge.id }
                            if (idx != -1 && !nudges[idx].isRead) {
                                nudges[idx] = nudges[idx].copy(isRead = true)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tap), // tap.png / tap.xml 있어야 함
                        contentDescription = "콕 찌르기 아이콘",
                        modifier = Modifier.size(25.dp),
                        tint = Color.Unspecified // 원본 색 유지
                    )

                    Spacer(Modifier.width(12.dp))

                    // 본문
                    Text(
                        nudge.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    // 1) 오른쪽 날짜
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatter.format(nudge.sentAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.4f))
            }
        }

        // 2) 모두 읽음 버튼
        Button(
            onClick = {
                for (i in nudges.indices) {
                    if (!nudges[i].isRead) {
                        nudges[i] = nudges[i].copy(isRead = true)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = main,
                contentColor = white
            )
        ) {
            Text("모두 읽음")
        }
    }
}
