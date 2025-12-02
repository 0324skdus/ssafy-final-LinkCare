package com.a307.linkcare.feature.notification.ui.list

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
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
import com.a307.linkcare.feature.notification.domain.model.response.NotificationType
import com.a307.linkcare.feature.notification.ui.NotificationViewModel
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
    val groupImages by viewModel.groupImages.collectAsState()

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
                    items(
                        items = notifications,
                        key = { it.notificationId }
                    ) { notification ->
                        // 로컬 상태로 처리 여부 관리
                        var isProcessed by remember(notification.notificationId) {
                            mutableStateOf(processedNotifications[notification.notificationId] != null)
                        }
                        var processedMessage by remember(notification.notificationId) {
                            mutableStateOf(processedNotifications[notification.notificationId] ?: "")
                        }

                        val textColor = if (notification.isRead) ReadGray else black

                        // 알림 타입에 따른 메시지
                        val message = if (isProcessed && processedMessage.isNotEmpty()) {
                            processedMessage
                        } else {
                            notification.content
                        }

                        // 삭제 처리 여부를 추적하는 로컬 상태
                        var isDismissed by remember { mutableStateOf(false) }

                        // 스와이프로 삭제
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart && !isDismissed) {
                                    isDismissed = true
                                    scope.launch {
                                        try {
                                            val result = viewModel.deleteNotification(notification.notificationId)
                                            result.onSuccess {
                                                Toast.makeText(context, "알림을 삭제했습니다", Toast.LENGTH_SHORT).show()
                                            }.onFailure { error ->
                                                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                                isDismissed = false
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "삭제 중 오류 발생", Toast.LENGTH_SHORT).show()
                                            isDismissed = false
                                        }
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
                                // 그룹 이미지
                                val imageUrl = notification.relatedGroupSeq?.let { groupSeq ->
                                    groupImages[groupSeq]
                                }

                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "그룹 이미지",
                                        modifier = Modifier
                                            .size(37.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 기본 아이콘
                                    Image(
                                        painter = painterResource(R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(37.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                }

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

                                val timeText = try {
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
                                    notification.createdAt.take(16)
                                }

                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                            }

                            // 요청일 때만 하단 버튼
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
                                                        Toast.makeText(context, "거절 실패", Toast.LENGTH_SHORT).show()
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
                                                        Toast.makeText(context, "승인 실패", Toast.LENGTH_SHORT).show()
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
