package com.a307.linkcare.feature.caregroup.ui.edit

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.component.molecules.avatar.GroupAvatarPicker
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.transformation.CropTransparentTransformation
import com.a307.linkcare.feature.caregroup.data.model.dto.GroupMember
import com.a307.linkcare.feature.commongroup.ui.home.MyGroupsViewModel
import com.a307.linkcare.feature.commongroup.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.mypage.ui.mypage.MyPageViewModel
import com.a307.linkcare.feature.mypage.data.model.response.GroupCharacterResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * URI를 임시 File로 변환하는 유틸 함수
 */
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "group_image_${System.currentTimeMillis()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()

        tempFile
    } catch (e: Exception) {
        null
    }
}
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGroupEdit(
    navController: NavHostController,
    groupSeq: Long,
    viewModel: CareGroupEditViewModel = hiltViewModel(),
    groupvm: MyGroupsViewModel? = null,
    myPageViewModel: MyPageViewModel = hiltViewModel(),
    onGroupDeleted: () -> Unit = {},
    onCopyInvite: (String) -> Unit = {},
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val memberActionState by viewModel.memberActionState.collectAsState()
    val context = LocalContext.current

    // 그룹 캐릭터 정보 로드
    val groupCharacters by myPageViewModel.groupCharacters.collectAsState()
    val characterMap = remember(groupCharacters) {
        groupCharacters.associateBy({ it.userId }, { it })
    }

    LaunchedEffect(groupSeq) {
        viewModel.loadGroupDetail(groupSeq)
        myPageViewModel.loadGroupCharacters(groupSeq)
    }

    // 위임하기 핸들러
    val onDelegate: (Long) -> Unit = { memberUserSeq ->
        viewModel.delegateLeader(groupSeq, memberUserSeq)
    }

    // 내보내기 핸들러
    val onKick: (Long) -> Unit = { memberUserSeq ->
        viewModel.kickMember(groupSeq, memberUserSeq)
    }

    // 탈퇴하기 핸들러
    val onLeave: () -> Unit = {
        viewModel.leaveGroup(groupSeq)
    }

    // 그룹 삭제 핸들러
    val onDelete: () -> Unit = {
        viewModel.deleteGroup(groupSeq)
    }

    // Handle member action state
    LaunchedEffect(memberActionState) {
        when (memberActionState) {
            is CareGroupEditViewModel.MemberActionState.DelegateSuccess -> {
                Toast.makeText(
                    context,
                    (memberActionState as CareGroupEditViewModel.MemberActionState.DelegateSuccess).msg,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetMemberActionState()
            }
            is CareGroupEditViewModel.MemberActionState.KickSuccess -> {
                Toast.makeText(
                    context,
                    (memberActionState as CareGroupEditViewModel.MemberActionState.KickSuccess).msg,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetMemberActionState()
            }
            is CareGroupEditViewModel.MemberActionState.LeaveSuccess -> {
                Toast.makeText(
                    context,
                    (memberActionState as CareGroupEditViewModel.MemberActionState.LeaveSuccess).msg,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetMemberActionState()
                // 그룹 목록 새로고침 후 care 탭의 루트로 이동
                CoroutineScope(Dispatchers.Main).launch {
                    groupvm?.loadCare()
                    onGroupDeleted()
                    navController.navigate("care/main") {
                        popUpTo(0) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            is CareGroupEditViewModel.MemberActionState.DeleteSuccess -> {
                Toast.makeText(
                    context,
                    (memberActionState as CareGroupEditViewModel.MemberActionState.DeleteSuccess).msg,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetMemberActionState()
                // 그룹 목록 새로고침 후 care 탭의 루트로 이동
                CoroutineScope(Dispatchers.Main).launch {
                    groupvm?.loadCare()
                    onGroupDeleted()
                    navController.navigate("care/main") {
                        popUpTo(0) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            is CareGroupEditViewModel.MemberActionState.Error -> {
                Toast.makeText(
                    context,
                    (memberActionState as CareGroupEditViewModel.MemberActionState.Error).msg,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetMemberActionState()
            }
            else -> {}
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is CareGroupEditViewModel.UpdateState.Success -> {
                Toast.makeText(context, "그룹 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateState()
                navController.popBackStack()
            }
            is CareGroupEditViewModel.UpdateState.Error -> {
                Toast.makeText(
                    context,
                    "수정 실패: ${(updateState as CareGroupEditViewModel.UpdateState.Error).msg}",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    when (val state = uiState) {
        is CareGroupEditViewModel.UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is CareGroupEditViewModel.UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("오류: ${state.msg}")
                    Spacer(Modifier.height(16.dp))
                    LcBtn(
                        text = "돌아가기",
                        onClick = onBack,
                        modifier = Modifier.width(120.dp).height(44.dp)
                    )
                }
            }
            return
        }
        is CareGroupEditViewModel.UiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is CareGroupEditViewModel.UiState.Success -> {
            RenderCareGroupEditForm(
                navController = navController,
                groupDetail = state.groupDetail,
                viewModel = viewModel,
                updateState = updateState,
                characterMap = characterMap,
                onDelegate = onDelegate,
                onKick = onKick,
                onLeave = onLeave,
                onDelete = onDelete,
                onCopyInvite = onCopyInvite,
                onBack = onBack
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun RenderCareGroupEditForm(
    navController: NavHostController,
    groupDetail: GroupDetailResponse,
    viewModel: CareGroupEditViewModel,
    updateState: CareGroupEditViewModel.UpdateState,
    characterMap: Map<Long, GroupCharacterResponse>,
    onDelegate: (memberId: Long) -> Unit,
    onKick: (memberId: Long) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onCopyInvite: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val groupSeq = groupDetail.groupSeq
    // 현재 로그인한 사용자가 그룹장인지 확인
    val isLeader = groupDetail.members.find { it.userSeq == groupDetail.currentUserSeq }?.isLeader ?: false

    // 초기값 프리필
    var name by remember { mutableStateOf(groupDetail.groupName) }
    var desc by remember { mutableStateOf(groupDetail.groupDescription) }
    var maxMemberText by remember { mutableStateOf(groupDetail.capacity.toString()) }

    // 이미지 관련 상태
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var shareExercise by remember { mutableStateOf(true) }
    var shareHr by remember { mutableStateOf(true) }
    var shareSteps by remember { mutableStateOf(true) }
    var shareSleep by remember { mutableStateOf(true) }
    var shareWater by remember { mutableStateOf(true) }
    var shareBp by remember { mutableStateOf(true) }
    var shareSugar by remember { mutableStateOf(true) }

    // 외부에서 groupDetail이 바뀌면 동기화
    LaunchedEffect(groupDetail) {
        name = groupDetail.groupName
        desc = groupDetail.groupDescription
        maxMemberText = groupDetail.capacity.toString()
        // TODO: load actual share settings from backend when available
    }

    val maxMember = maxMemberText.toIntOrNull()
    val maxValid = maxMember != null && maxMember in 1..6
    val requiredAllChecked = shareExercise && shareHr && shareSteps
    val canSubmit = isLeader &&
            name.isNotBlank() && maxValid && requiredAllChecked

    val snackbarHostState = remember { SnackbarHostState() }

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
                        defaultRes = R.drawable.main_duck,
                        imageUrl = groupDetail.imageUrl,
                        onImagePicked = { uri ->
                            if (isLeader) {
                                selectedImageUri = uri
                            }
                        }
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

            // 구성원
            item {
                SectionTitle("구성원")
                Spacer(Modifier.height(12.dp))
            }
            itemsIndexed(
                groupDetail.members,
                key = { index, m -> m.userSeq }
            ) { _, member ->
                val charInfo = characterMap[member.userSeq]
                val groupMember = GroupMember(
                    id = member.userSeq,
                    name = member.userName,
                    avatarRes = null,
                    isLeader = member.isLeader,
                    avatarUrl = charInfo?.mainCharacterImageUrl ?: member.mainCharacterBaseImageUrl
                )
                MemberRow(
                    member = groupMember,
                    isLeader = isLeader,
                    backgroundUrl = charInfo?.mainBackgroundImageUrl,
                    onDelegate = { onDelegate(member.userSeq) },
                    onKick = { onKick(member.userSeq) }
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
                        ToggleRow("운동", shareExercise, onChange = { if (isLeader) shareExercise = it }, enabled = true, isRequired = true)
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = Color(0xFFE5E5EA)
                        )
                        ToggleRow("심박수", shareHr, onChange = { if (isLeader) shareHr = it }, enabled = true, isRequired = true)
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = Color(0xFFE5E5EA)
                        )
                        ToggleRow("걸음 수", shareSteps, onChange = { if (isLeader) shareSteps = it }, enabled = true, isRequired = true)

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "선택 항목",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        ToggleRow("수면", shareSleep, onChange = { if (isLeader) shareSleep = it }, enabled = true, isRequired = false)
                        ToggleRow("음수량", shareWater, onChange = { if (isLeader) shareWater = it }, enabled = true, isRequired = false)
                        ToggleRow("혈압", shareBp, onChange = { if (isLeader) shareBp = it }, enabled = true, isRequired = false)
                        ToggleRow("혈당", shareSugar, onChange = { if (isLeader) shareSugar = it }, enabled = true, isRequired = false)
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
                    // 그룹장: [수정하기]/[그룹 삭제하기]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LcBtn(
                            text = "수정하기",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            buttonColor = main,
                            buttonTextColor = white,
                            isEnabled = canSubmit && updateState !is CareGroupEditViewModel.UpdateState.Loading,
                            onClick = {
                                val imageAction = if (selectedImageUri != null) "update" else "keep"
                                val imageFile = selectedImageUri?.let { uri ->
                                    uriToFile(context, uri)
                                }

                                viewModel.updateGroup(
                                    groupSeq = groupSeq,
                                    groupName = name.trim(),
                                    groupDescription = desc.trim(),
                                    imageAction = imageAction,
                                    imageFile = imageFile,
                                    isSleepRequired = shareSleep,
                                    isWaterIntakeRequired = shareWater,
                                    isBloodPressureRequired = shareBp,
                                    isBloodSugarRequired = shareSugar
                                )
                            }
                        )
                        LcBtn(
                            text = "그룹 삭제하기",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            buttonColor = Color(0xFFFF6B6B),
                            buttonTextColor = white,
                            isEnabled = true,
                            onClick = onDelete
                        )
                    }
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
    backgroundUrl: String? = null,
    onDelegate: () -> Unit,
    onKick: () -> Unit
) {
    val context = LocalContext.current

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
                // 아바타 (배경 이미지 + 캐릭터 이미지)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F3F6)),
                    contentAlignment = Alignment.Center
                ) {
                    // 배경 이미지
                    if (backgroundUrl != null) {
                        AsyncImage(
                            model = backgroundUrl,
                            contentDescription = "배경",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // 캐릭터 이미지
                    if (member.avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(member.avatarUrl)
                                .transformations(CropTransparentTransformation())
                                .size(Size.ORIGINAL)
                                .build(),
                            contentDescription = "캐릭터",
                            modifier = Modifier.size(35.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
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
                        Button(
                            onClick = onDelegate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = main,
                                contentColor = white
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("위임하기", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onKick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B),
                                contentColor = white
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("내보내기", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
