package com.a307.linkcare.feature.caregroup

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.caregroup.domain.ShareOptions
import kotlinx.coroutines.launch

// ====== 예시 도메인 ======
data class CareGroup(
    val id: Long,
    val name: String,
    val description: String,
    val maxMember: Int,
    val share: ShareOptions,
    val inviteLink: String,
    val members: List<GroupMember>,
    @DrawableRes val avatarRes: Int? = null
)

data class GroupMember(
    val id: Long,
    val name: String,
    @DrawableRes val avatarRes: Int?,
    val isLeader: Boolean
)
// ========================

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGroupEdit(
    navController: NavHostController,
    initial: CareGroup,
    isLeader: Boolean,
    onUpdate: (updated: CareGroup) -> Unit = {},
    onDelegate: (memberId: Long) -> Unit = {},
    onKick: (memberId: Long) -> Unit = {},
    onLeave: () -> Unit = {},
    onCopyInvite: (String) -> Unit = {},
    onBack: () -> Unit = { navController.popBackStack() }
) {
    // ---- 내부 상태: 초기값 프리필 ----
    var name by remember { mutableStateOf(initial.name) }
    var desc by remember { mutableStateOf(initial.description) }
    var maxMemberText by remember { mutableStateOf(initial.maxMember.toString()) }

    var shareExercise by remember { mutableStateOf(initial.share.exercise) }
    var shareHr by remember { mutableStateOf(initial.share.heartRate) }
    var shareSteps by remember { mutableStateOf(initial.share.steps) }
    var shareSleep by remember { mutableStateOf(initial.share.sleep) }
    var shareWater by remember { mutableStateOf(initial.share.water) }
    var shareBp by remember { mutableStateOf(initial.share.bloodPressure) }
    var shareSugar by remember { mutableStateOf(initial.share.bloodSugar) }

    // 외부에서 initial이 바뀌면 동기화
    LaunchedEffect(initial) {
        name = initial.name
        desc = initial.description
        maxMemberText = initial.maxMember.toString()
        shareExercise = initial.share.exercise
        shareHr = initial.share.heartRate
        shareSteps = initial.share.steps
        shareSleep = initial.share.sleep
        shareWater = initial.share.water
        shareBp = initial.share.bloodPressure
        shareSugar = initial.share.bloodSugar
    }

    val maxMember = maxMemberText.toIntOrNull()
    val maxValid = maxMember != null && maxMember in 1..6
    val requiredAllChecked = shareExercise && shareHr && shareSteps
    val canSubmit = isLeader &&
            name.isNotBlank() && maxValid && requiredAllChecked

    val clipboard: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 그룹 이미지
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    GroupAvatarPicker(
                        defaultRes = initial.avatarRes ?: R.drawable.main_duck,
                        onImagePicked = { /* TODO: 이미지 변경 핸들러 필요시 연결 */ }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isLeader) "그룹 사진 변경" else "그룹 사진",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            // 그룹명
            item {
                SectionTitle("그룹명")
                Spacer(Modifier.height(8.dp))
                LcInputField(
                    value = name,
                    onValueChange = { if (isLeader) name = it },
                    placeholder = "그룹명을 입력해주세요",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    backgroundColor = Color(0xFFF5F6FA)
                )
            }

            // 그룹 설명
            item {
                SectionTitle("그룹 설명")
                Spacer(Modifier.height(8.dp))
                LcInputField(
                    value = desc,
                    onValueChange = { if (isLeader) desc = it },
                    placeholder = "그룹에 대한 설명을 작성해주세요",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    singleLine = false,
                    backgroundColor = Color(0xFFF5F6FA)
                )
            }

            // 최대 인원
            item {
                SectionTitle("최대 인원")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "최대 6명까지 선택 가능합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LcInputField(
                        value = maxMemberText,
                        onValueChange = { s ->
                            if (!isLeader) return@LcInputField
                            if (s.isEmpty()) maxMemberText = s
                            else if (s.length <= 1 && s.all { it.isDigit() } && s.toInt() in 1..6)
                                maxMemberText = s
                        },
                        placeholder = "6",
                        modifier = Modifier.width(88.dp),
                        singleLine = true,
                        backgroundColor = Color(0xFFF5F6FA)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("명", style = MaterialTheme.typography.titleMedium, color = black)
                }
                if (isLeader && !maxValid && maxMemberText.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "1~6 사이의 숫자만 입력해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9534F)
                    )
                }
            }

            // 초대 링크
            item {
                SectionTitle("초대 링크")
                Spacer(Modifier.height(8.dp))
                InviteLinkRow(
                    link = initial.inviteLink,
                    onCopy = {
                        clipboard.setText(AnnotatedString(initial.inviteLink))
                        onCopyInvite(initial.inviteLink)
                        scope.launch {
                            snackbarHostState.showSnackbar("초대 링크가 복사되었어요.")
                        }
                    }
                )
            }

            // 구성원
            item {
                SectionTitle("구성원")
                Spacer(Modifier.height(12.dp))
            }
            itemsIndexed(initial.members, key = { index, m -> m.id ?: "idx_$index" }) { _, m ->
                MemberRow(
                    member = m,
                    isLeader = isLeader,
                    onDelegate = { onDelegate(m.id) },
                    onKick = { onKick(m.id) }
                )
                Spacer(Modifier.height(12.dp))
            }

            // 공유 데이터 설정
            item {
                SectionTitle("공유 데이터 설정")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "그룹원들과 공유할 건강 데이터를 선택해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FA))
                ) {
                    Column(Modifier
                        .fillMaxWidth()
                        .padding(12.dp)) {
                        ToggleRow("운동", shareExercise, { if (isLeader) shareExercise = it }, true, true)
                        Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        ToggleRow("심박수", shareHr, { if (isLeader) shareHr = it }, true, true)
                        Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        ToggleRow("걸음 수", shareSteps, { if (isLeader) shareSteps = it }, true, true)

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "선택 항목",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        ToggleRow("수면", shareSleep, { if (isLeader) shareSleep = it }, true, false)
                        ToggleRow("음수량", shareWater, { if (isLeader) shareWater = it }, true, false)
                        ToggleRow("혈압", shareBp, { if (isLeader) shareBp = it }, true, false)
                        ToggleRow("혈당", shareSugar, { if (isLeader) shareSugar = it }, true, false)
                    }
                }

                if (isLeader && !requiredAllChecked) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "필수 항목(운동, 심박수, 걸음 수)을 모두 켜주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9534F)
                    )
                }
            }

            // 하단 버튼
            item {
                if (isLeader) {
                    // 그룹장: [수정하기] 단일 버튼
                    LcBtn(
                        text = "수정하기",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        buttonColor = main,
                        buttonTextColor = white,
                        isEnabled = canSubmit,
                        onClick = {
                            val updated = initial.copy(
                                name = name.trim(),
                                description = desc.trim(),
                                maxMember = maxMember ?: initial.maxMember,
                                share = ShareOptions(
                                    exercise = shareExercise,
                                    heartRate = shareHr,
                                    steps = shareSteps,
                                    sleep = shareSleep,
                                    water = shareWater,
                                    bloodPressure = shareBp,
                                    bloodSugar = shareSugar
                                )
                            )
                            onUpdate(updated)
                            navController.popBackStack()
                        }
                    )
                } else {
                    // 일반 구성원: [돌아가기]/[탈퇴하기]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LcBtn(
                            text = "돌아가기",
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            buttonColor = Color(0xFFE9ECEF),
                            buttonTextColor = black,
                            isEnabled = true,
                            onClick = onBack
                        )
                        LcBtn(
                            text = "탈퇴하기",
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            buttonColor = Color(0xFF6C757D),
                            buttonTextColor = white,
                            isEnabled = true,
                            onClick = onLeave
                        )
                    }
                }
            }
        }
    }
}

/** 제목 공용 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = black
    )
}

/** 초대 링크 행 */
@Composable
private fun InviteLinkRow(
    link: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LcInputField(
            value = link,
            onValueChange = { /* read-only */ },
            placeholder = "",
            modifier = Modifier.weight(1f),
            singleLine = true,
            backgroundColor = Color(0xFFF5F6FA)
        )
        Spacer(Modifier.width(8.dp))
        LcBtn(
            text = "복사",
            modifier = Modifier
                .width(72.dp)
                .height(44.dp),
            buttonColor = Color(0xFFDEE2E6),
            buttonTextColor = black,
            isEnabled = true,
            onClick = onCopy
        )
    }
}

/** 구성원 행: 그룹장일 때만 위임/내보내기 표시 */
@Composable
private fun MemberRow(
    member: GroupMember,
    isLeader: Boolean,
    onDelegate: () -> Unit,
    onKick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // 살짝 줄여서 행 높이도 낮춤
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아바타
                Box(modifier = Modifier.size(44.dp)) {
                    GroupAvatarChip(avatarRes = member.avatarRes ?: R.drawable.main_duck)
                }

                Spacer(Modifier.width(10.dp))

                // 이름/역할
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (member.isLeader) "그룹장" else "그룹원",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF868E96)
                    )
                }

                // 오른쪽 작은 액션들
                if (isLeader && !member.isLeader) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = onDelegate,
                            label = { Text("위임하기") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = main,
                                labelColor = white
                            )
                        )
                        AssistChip(
                            onClick = onKick,
                            label = { Text("내보내기") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFF6B6B),
                                labelColor = white
                            )
                        )
                    }
                }
            }
        }
    }
}


/** 아바타 간단 칩 */
@Composable
fun GroupAvatarChip(@DrawableRes avatarRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .background(Color.Transparent)
    ) {
        GroupAvatarPickerEdit(defaultRes = avatarRes, onImagePicked = {}, enabled = false)
    }
}
