package com.ssafy.sdk.health.presentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.sdk.health.presentation.ui.PermissionScreen

@Composable
fun HealthSyncScreen(
    userId: Int,
    viewModel: HealthSyncViewModel = hiltViewModel()
) {
    val syncProgress by viewModel.syncProgress.collectAsState()
    val activity = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val progress = syncProgress) {
            is SyncProgress.Idle -> {
                IdleContent(
                    onSyncClick = { viewModel.syncHealthData(userId) },
                    onForceFullSyncClick = { viewModel.forceFullSync(userId) },
                    onExerciseSyncClick = { viewModel.syncExerciseOnly(userId) }
                )
            }

            is SyncProgress.PermissionRequired -> {
                // 새로운 권한 화면 사용
                PermissionScreen(
                    viewModel = viewModel,
                    activity = activity
                )
            }

            is SyncProgress.CollectingData -> {
                LoadingContent("데이터 수집 중...")
            }

            is SyncProgress.FullSyncInProgress -> {
                FullSyncProgressContent(
                    dataType = progress.dataType,
                    current = progress.current,
                    total = progress.total,
                    percentage = progress.percentage
                )
            }

            is SyncProgress.Uploading -> {
                LoadingContent(progress.message)
            }

            is SyncProgress.Success -> {
                SuccessContent(
                    message = progress.message,
                    onDismiss = { viewModel.resetSyncStatus() }
                )
            }

            is SyncProgress.Error -> {
                ErrorContent(
                    message = progress.message,
                    onRetry = { viewModel.syncHealthData(userId) },
                    onDismiss = { viewModel.resetSyncStatus() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    onSyncClick: () -> Unit,
    onForceFullSyncClick: () -> Unit,
    onExerciseSyncClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "건강 데이터 동기화",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = onSyncClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("동기화 시작")
        }

        OutlinedButton(
            onClick = onForceFullSyncClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("전체 재동기화")
        }

        OutlinedButton(
            onClick = onExerciseSyncClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("오늘 운동 데이터만 동기화")
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(message)
    }
}

@Composable
private fun FullSyncProgressContent(
    dataType: String,
    current: Int,
    total: Int,
    percentage: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "$dataType 동기화 중...",
            style = MaterialTheme.typography.titleLarge
        )

        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Text("$current / $total ($percentage%)")
    }
}

@Composable
private fun SuccessContent(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "✅ $message",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Button(onClick = onDismiss) {
            Text("확인")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "❌ 오류 발생",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )

        Text(message)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("닫기")
            }

            Button(onClick = onRetry) {
                Text("재시도")
            }
        }
    }
}
