package com.a307.linkcare.feature.notification

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R // << 여기!
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.util.painterResourceCropped
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LetterList(
    viewModel: NotificationViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }
    val readGray = Color(0xFF9E9E9E)

    // ViewModel에서 편지 데이터 가져오기
    val letterAlarms by viewModel.letterAlarms.collectAsState()

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
            items(items = letterAlarms, key = { it.alarmId }) { letter ->
                val textColor = if (letter.read) readGray else black

                // 삭제 처리 여부를 추적하는 로컬 상태
                var isDismissed by remember { mutableStateOf(false) }

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart && !isDismissed) {
                            isDismissed = true
                            scope.launch {
                                viewModel.deleteAlarm(letter.alarmId)
                                Toast.makeText(context, "알림을 삭제했습니다", Toast.LENGTH_SHORT).show()
                            }
                            true
                        } else {
                            false
                        }
                    },
                    positionalThreshold = { it * 0.5f }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // currentValue를 사용하여 실제 스와이프 상태 확인
                        val color = when (dismissState.currentValue) {
                            SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF6B6B)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
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
                                    // 항목 탭 → 읽음 처리
                                    if (!letter.read) {
                                        viewModel.markAlarmAsRead(letter.alarmId)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1) 편지 아이콘 (기본 아이콘 사용)
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = "${letter.senderInfo.nickname} 캐릭터",
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(CircleShape),
                                tint = Color.Unspecified
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                // 첫 번째 줄: 발신자 (볼드)
                                Text(
                                    text = letter.senderInfo.nickname,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(Modifier.height(4.dp))
                                // 두 번째 줄: 내용 (2줄까지)
                                Text(
                                    text = letter.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = textColor
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            // 2) 우측 끝 날짜
                            Text(
                                text = parseAndFormatDate(letter.sentAt, formatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // 3) 모두 읽음 버튼
        Button(
            onClick = {
                viewModel.markAllAlarmsAsRead()
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
