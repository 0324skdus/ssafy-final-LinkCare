package com.a307.linkcare.feature.notification

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.util.painterResourceCropped
import com.a307.linkcare.feature.notification.domain.NotificationType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
fun GroupList(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }

    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = main)
                }
            }
            notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("알림이 없습니다", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(items = notifications, key = { it.notificationId }) { notification ->
                        val textColor = if (notification.isRead) ReadGray else black

                        // 알림 타입에 따른 메시지
                        val message = notification.content

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable {
                                    if (!notification.isRead) {
                                        viewModel.markAsRead(notification.notificationId)
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

                                // 시간 파싱 (ISO 형식 가정)
                                val timeText = try {
                                    val dateTime = LocalDateTime.parse(
                                        notification.createdAt,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                    )
                                    val displayFormatter = DateTimeFormatter.ofPattern("MM.dd HH:mm")
                                    dateTime.format(displayFormatter)
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
                                && notification.relatedRequestSeq != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.rejectJoinRequest(notification.relatedRequestSeq)
                                                    .onSuccess {
                                                        Toast.makeText(context, "가입 신청을 거절했습니다", Toast.LENGTH_SHORT).show()
                                                        viewModel.loadNotifications("GROUP")
                                                    }
                                                    .onFailure { error ->
                                                        Toast.makeText(context, "거절 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        },
                                        border = BorderStroke(1.dp, Color(0x33FF6B35)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("거절", color = Color(0xFFFF6B35)) }

                                    Spacer(Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.approveJoinRequest(notification.relatedRequestSeq)
                                                    .onSuccess {
                                                        Toast.makeText(context, "가입 신청을 승인했습니다", Toast.LENGTH_SHORT).show()
                                                        viewModel.loadNotifications("GROUP")
                                                    }
                                                    .onFailure { error ->
                                                        Toast.makeText(context, "승인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    }
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
            }
        }

        // 모두 읽음 버튼 (모든 when 블록 밖)
        if (notifications.any { !it.isRead }) {
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
