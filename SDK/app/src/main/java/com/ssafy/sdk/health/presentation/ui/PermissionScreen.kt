package com.ssafy.sdk.health.presentation.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.sdk.health.presentation.HealthSyncViewModel

/**
 * 권한이 필요할 때 보여주는 화면
 */
@Composable
fun PermissionScreen(
    viewModel: HealthSyncViewModel,
    activity: Activity
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 경고 아이콘과 제목
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "권한 필요",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 설명 텍스트
        Text(
            text = "삼성헬스 접근 권한이 필요합니다.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 필요한 권한 목록
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "필요한 권한:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PermissionItem("걸음수")
                PermissionItem("심박수")
                PermissionItem("수면")
                PermissionItem("운동")
                PermissionItem("음수량")
                PermissionItem("혈압")
                PermissionItem("활동 요약")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 권한 요청 버튼
        Button(
            onClick = {
                viewModel.requestPermissions(activity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("권한 설정하기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 안내 텍스트
        Text(
            text = "삼성헬스 앱에서 권한을 허용해주세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}