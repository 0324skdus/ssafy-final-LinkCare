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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotiList(
    viewModel: NotificationViewModel
) {
    // 예: "11.06 14:35"
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }
    val readGray = Color(0xFF9E9E9E)

    // ViewModel에서 콕 찌르기 데이터 가져오기
    val pokeAlarms by viewModel.pokeAlarms.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(items = pokeAlarms, key = { it.alarmId }) { alarm ->
                val textColor = if (alarm.read) readGray else black

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = white)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // 항목 터치 시 읽음 처리
                                if (!alarm.read) {
                                    viewModel.markAlarmAsRead(alarm.alarmId)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tap),
                            contentDescription = "콕 찌르기 아이콘",
                            modifier = Modifier.size(37.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // 첫 번째 줄: 발신자 (볼드)
                            Text(
                                text = alarm.senderInfo.nickname,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            // 두 번째 줄: 내용
                            Text(
                                text = alarm.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // 오른쪽 날짜
                        Text(
                            text = parseAndFormatDate(alarm.sentAt, formatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                }
            }
        }

        // 2) 모두 읽음 버튼
        Button(
            onClick = {
                viewModel.markAllAlarmsAsRead()
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

// ISO 8601 날짜 파싱 헬퍼 함수
private fun parseAndFormatDate(isoDate: String, formatter: SimpleDateFormat): String {
    return try {
        // 여러 형식 시도
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",  // 마이크로초 포함 (Z 없음)
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // 밀리초 + Z
            "yyyy-MM-dd'T'HH:mm:ss.SSS",     // 밀리초 (Z 없음)
            "yyyy-MM-dd'T'HH:mm:ss'Z'",      // 초 + Z
            "yyyy-MM-dd'T'HH:mm:ss"          // 초 (Z 없음)
        )

        var date: java.util.Date? = null
        for (format in formats) {
            try {
                val isoFormatter = SimpleDateFormat(format, Locale.US)
                isoFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                date = isoFormatter.parse(isoDate)
                if (date != null) break
            } catch (e: Exception) {
                continue
            }
        }

        // Date → "MM.dd HH:mm" (한국 시간대로)
        if (date != null) {
            formatter.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            formatter.format(date)
        } else {
            isoDate
        }
    } catch (e: Exception) {
        isoDate
    }
}
