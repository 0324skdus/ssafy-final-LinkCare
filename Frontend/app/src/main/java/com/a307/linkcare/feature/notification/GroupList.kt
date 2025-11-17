package com.a307.linkcare.feature.notification

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.feature.notification.domain.NotificationType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReadGray = Color(0xFF9E9E9E)

@Composable
fun GroupList(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }

    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val processedNotifications by viewModel.processedNotifications.collectAsState()

    // 디버깅: notifications 변경 감지
    LaunchedEffect(notifications.size) {
        android.util.Log.d("GroupList", "📋 UI 재구성: ${notifications.size}개 알림")
    }

    // 알림 로드
    LaunchedEffect(Unit) {
        viewModel.loadNotifications("GROUP")
    }

    // 에러 메시지 표시
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = main)
                }
            }
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("알림이 없습니다", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(items = notifications) { notification ->
                        // 로컬 상태로 처리 여부 관리
                        var isProcessed by remember(notification.notificationId) {
                            mutableStateOf(processedNotifications[notification.notificationId] != null)
                        }
                        var processedMessage by remember(notification.notificationId) {
                            mutableStateOf(processedNotifications[notification.notificationId] ?: "")
                        }

                        val textColor = if (notification.isRead) ReadGray else black

                        // 알림 타입에 따른 메시지 - 처리되었으면 처리 메시지, 아니면 원본
                        val message = if (isProcessed && processedMessage.isNotEmpty()) {
                            processedMessage
                        } else {
                            notification.content
                        }

                        // 스와이프로 삭제
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        try {
                                            android.util.Log.d("GroupList", "알림 삭제 시도: ${notification.notificationId}")
                                            val result = viewModel.deleteNotification(notification.notificationId)
                                            result.onSuccess {
                                                android.util.Log.d("GroupList", "알림 삭제 성공")
                                                Toast.makeText(context, "알림을 삭제했습니다", Toast.LENGTH_SHORT).show()
                                            }.onFailure { error ->
                                                android.util.Log.e("GroupList", "알림 삭제 실패: ${error.message}", error)
                                                Toast.makeText(context, "삭제 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("GroupList", "알림 삭제 예외: ${e.message}", e)
                                            Toast.makeText(context, "삭제 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    true // 스와이프 확정 (UI에서 일단 제거, 실패하면 에러 표시)
                                } else {
                                    false // 스와이프 취소
                                }
                            },
                            positionalThreshold = { it * 0.5f } // 절반 이상 스와이프해야 삭제
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
                                    .clickable(
                                        enabled = notification.type != NotificationType.GROUP_JOIN_REQUEST
                                    ) {
                                        if (!notification.isRead) {
                                            viewModel.markAsRead(notification.notificationId)
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 기본 아이콘
                                Image(
                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(37.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notification.title,
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

                                // 시간 파싱 및 한국 시간대로 변환
                                val timeText = try {
                                    // UTC 시간으로 파싱
                                    val utcDateTime = if (notification.createdAt.endsWith("Z")) {
                                        ZonedDateTime.parse(notification.createdAt)
                                    } else {
                                        LocalDateTime.parse(
                                            notification.createdAt,
                                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                        ).atZone(ZoneId.of("UTC"))
                                    }

                                    // 한국 시간대(Asia/Seoul)로 변환
                                    val kstDateTime = utcDateTime.withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                    val displayFormatter = DateTimeFormatter.ofPattern("MM.dd HH:mm")
                                    kstDateTime.format(displayFormatter)
                                } catch (e: Exception) {
                                    android.util.Log.e("GroupList", "시간 파싱 실패: ${notification.createdAt}", e)
                                    notification.createdAt.take(16)
                                }

                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                            }

                            // 요청일 때만 하단 버튼 (처리되지 않았을 때만 표시)
                            if (notification.type == NotificationType.GROUP_JOIN_REQUEST
                                && notification.relatedRequestSeq != null
                                && !isProcessed) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                // 읽음 처리
                                                if (!notification.isRead) {
                                                    viewModel.markAsRead(notification.notificationId)
                                                }
                                                // 신청자 이름 추출
                                                val userName = notification.content.substringBefore("님이")
                                                // 거절 처리
                                                viewModel.rejectJoinRequest(
                                                    notification.relatedRequestSeq,
                                                    notification.notificationId,
                                                    userName
                                                )
                                                    .onSuccess {
                                                        // 로컬 상태 업데이트
                                                        processedMessage = "${userName}님 신청을 거절하였습니다"
                                                        isProcessed = true
                                                        Toast.makeText(context, "가입 신청을 거절했습니다", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .onFailure { error ->
                                                        Toast.makeText(context, "거절 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        },
                                        border = BorderStroke(1.dp, Color(0xFFFF6B35)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("거절", color = Color(0xFFFF6B35)) }

                                    Spacer(Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                // 읽음 처리
                                                if (!notification.isRead) {
                                                    viewModel.markAsRead(notification.notificationId)
                                                }
                                                // 신청자 이름 추출
                                                val userName = notification.content.substringBefore("님이")
                                                // 승인 처리
                                                viewModel.approveJoinRequest(
                                                    notification.relatedRequestSeq,
                                                    notification.notificationId,
                                                    userName
                                                )
                                                    .onSuccess {
                                                        // 로컬 상태 업데이트
                                                        processedMessage = "${userName}님 신청을 수락하였습니다"
                                                        isProcessed = true
                                                        Toast.makeText(context, "가입 신청을 승인했습니다", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .onFailure { error ->
                                                        Toast.makeText(context, "승인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        },
                                        border = BorderStroke(1.dp, Color(0xFF8ECFA9)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("수락", color = Color(0xFF8ECFA9)) }
                                }
                            }

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        // 모두 읽음 버튼 (알림이 있으면 항상 표시)
        if (notifications.isNotEmpty()) {
            Button(
                onClick = {
                    viewModel.markAllAsRead()
                    Toast.makeText(context, "모든 알림을 읽음 처리했습니다", Toast.LENGTH_SHORT).show()
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
