package com.a307.linkcare.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.theme.black

data class GroupAlert(val groupName: String, val message: String, val success: Boolean)

@Composable
fun GroupList() {
    // 상태 리스트 (읽음 처리 시 제거)
    val alerts = remember {
        mutableStateListOf(
            GroupAlert("라연언전가", "김도훈님이 걷기 신호를 보냈습니다.", true),
            GroupAlert("곡연안전가", "김도훈님이 걷기 신호를 보냈습니다.", false)
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
            items(alerts) { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = white)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "그룹 아이콘",
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF4A89F6)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                alert.groupName,
                                fontWeight = FontWeight.Bold,
                                color = black
                            )
                            Text(
                                alert.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (alert.success) {
                            Button(
                                onClick = { alerts.remove(alert) }, // 개별 삭제
                                colors = ButtonDefaults.buttonColors(Color(0xFF8ECFA9))
                            ) {
                                Text("수락", color = black)
                            }
                        } else {
                            Button(
                                onClick = { alerts.remove(alert) }, // 개별 삭제
                                colors = ButtonDefaults.buttonColors(Color(0xFFFF6B35))
                            ) {
                                Text("거절", color = black)
                            }
                        }
                    }
                }
            }
        }

        // ✅ 읽을 게 있을 때만 버튼 표시
        if (alerts.isNotEmpty()) {
            Button(
                onClick = { alerts.clear() }, // 모두 삭제 (읽음 처리)
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
}
