package com.a307.linkcare.feature.commongroup.ui.invite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.commongroup.ui.invite.InvitationViewModel
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationPreviewResponse
import com.a307.linkcare.feature.commongroup.data.model.dto.PermissionAgreementDto

@Composable
fun InvitationPreviewScreen(
    invitationToken: String,
    navController: NavController,
    vm: InvitationViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(invitationToken) {
        vm.loadPreview(invitationToken)
    }

    LaunchedEffect(uiState) {
        if (uiState is InvitationViewModel.UiState.JoinSuccess) {
            // 참가 성공 시 메인 화면으로 이동
            navController.navigate("main") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is InvitationViewModel.UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = main)
                }
            }

            is InvitationViewModel.UiState.Preview -> {
                InvitationPreviewContent(
                    preview = state.data,
                    onJoinClick = { permissions ->
                        vm.joinGroup(invitationToken, permissions)
                    },
                    onCancelClick = {
                        navController.popBackStack()
                    }
                )
            }

            is InvitationViewModel.UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onBackClick = { navController.popBackStack() }
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun InvitationPreviewContent(
    preview: InvitationPreviewResponse,
    onJoinClick: (PermissionAgreementDto) -> Unit,
    onCancelClick: () -> Unit
) {
    // 권한 동의 상태
    var agreedSleep by remember { mutableStateOf(preview.optionalPermissions?.isSleepAllowed ?: false) }
    var agreedWater by remember { mutableStateOf(preview.optionalPermissions?.isWaterIntakeAllowed ?: false) }
    var agreedBp by remember { mutableStateOf(preview.optionalPermissions?.isBloodPressureAllowed ?: false) }
    var agreedSugar by remember { mutableStateOf(preview.optionalPermissions?.isBloodSugarAllowed ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 그룹 정보
        Text(
            text = "그룹 초대",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = black
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = unActiveField)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = preview.groupName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = black
                )

                Text(
                    text = preview.groupDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip("${preview.type}")
                    InfoChip("${preview.currentMembers}/${preview.capacity}명")
                }
            }
        }

        // 경고 메시지
        if (preview.isExpired) {
            ErrorCard("이 초대 링크는 만료되었습니다")
        } else if (preview.isFull) {
            ErrorCard("그룹 정원이 가득 찼습니다")
        }

        // 필수 권한
        Text(
            text = "필수 권한 (자동 동의)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = black
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionItem("걸음수 데이터", true, enabled = false)
                PermissionItem("심박수 데이터", true, enabled = false)
                PermissionItem("운동 데이터", true, enabled = false)
            }
        }

        // 선택 권한 (케어 그룹만)
        if (preview.type == "CARE" && preview.optionalPermissions != null) {
            Text(
                text = "선택 권한",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = black
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preview.optionalPermissions.isSleepAllowed?.let {
                        if (it) PermissionItem("수면 데이터", agreedSleep) { agreedSleep = it }
                    }
                    preview.optionalPermissions.isWaterIntakeAllowed?.let {
                        if (it) PermissionItem("음수량 데이터", agreedWater) { agreedWater = it }
                    }
                    preview.optionalPermissions.isBloodPressureAllowed?.let {
                        if (it) PermissionItem("혈압 데이터", agreedBp) { agreedBp = it }
                    }
                    preview.optionalPermissions.isBloodSugarAllowed?.let {
                        if (it) PermissionItem("혈당 데이터", agreedSugar) { agreedSugar = it }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 버튼
        val canJoin = !preview.isExpired && !preview.isFull

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancelClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = unActiveBtn,
                    contentColor = black
                )
            ) {
                Text("취소")
            }

            Button(
                onClick = {
                    val permissions = PermissionAgreementDto(
                        isSleepAllowed = agreedSleep,
                        isWaterIntakeAllowed = agreedWater,
                        isBloodPressureAllowed = agreedBp,
                        isBloodSugarAllowed = agreedSugar
                    )
                    onJoinClick(permissions)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = main,
                    contentColor = white
                ),
                enabled = canJoin
            ) {
                Text("참가하기")
            }
        }
    }
}

@Composable
private fun PermissionItem(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) black else Color.Gray
        )

        if (onCheckedChange != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = main,
                    uncheckedColor = Color.Gray
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = main
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = main.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = main
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "X",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier.height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = main,
                contentColor = white
            )
        ) {
            Text("돌아가기")
        }
    }
}
