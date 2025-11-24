package com.a307.linkcare.feature.caregroup.ui.create

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.feature.caregroup.data.model.request.ShareOptions
import com.a307.linkcare.R
import com.a307.linkcare.common.component.molecules.avatar.GroupAvatarPicker
import com.a307.linkcare.feature.commongroup.ui.home.MyGroupsViewModel
import com.a307.linkcare.feature.healthgroup.ui.create.toMultipart


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGroupCreate(
    navController: NavHostController,
    onClickCreate: (
        share: ShareOptions
    ) -> Unit = { _ -> },
    @DrawableRes avatarRes: Int? = null,
    vm: CareGroupCreateViewModel = hiltViewModel(),
    groupvm: MyGroupsViewModel
) {
    val state by vm.state
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 입력 상태
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var maxMemberText by remember { mutableStateOf("") }

    // 토글 상태
    var shareExercise by remember { mutableStateOf(true) }
    var shareHr by remember { mutableStateOf(true) }
    var shareSteps by remember { mutableStateOf(true) }
    var shareSleep by remember { mutableStateOf(false) }
    var shareWater by remember { mutableStateOf(false) }
    var shareBp by remember { mutableStateOf(false) }
    var shareSugar by remember { mutableStateOf(false) }

    val maxMember = maxMemberText.toIntOrNull()
    val maxValid = maxMember != null && maxMember in 1..6
    val requiredAllChecked = shareExercise && shareHr && shareSteps
    val canSubmit = name.isNotBlank() && maxValid && requiredAllChecked

    LaunchedEffect(state) {
        when (state) {
            is CareGroupCreateViewModel.UiState.Success -> {
                Toast.makeText(context, "생성 완료!", Toast.LENGTH_SHORT).show()

                // 그룹 목록 새로고침
                groupvm.loadCare()

                // 그룹 생성 성공했을 때만 ShareOptions 전달
                onClickCreate(
                    ShareOptions(
                        exercise = shareExercise,
                        heartRate = shareHr,
                        steps = shareSteps,
                        sleep = shareSleep,
                        water = shareWater,
                        bloodPressure = shareBp,
                        bloodSugar = shareSugar
                    )
                )

                navController.popBackStack()
            }

            is CareGroupCreateViewModel.UiState.Error -> {
                Toast.makeText(
                    context,
                    "에러: ${(state as CareGroupCreateViewModel.UiState.Error).msg}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> Unit
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 그룹 이미지
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GroupAvatarPicker(defaultRes = avatarRes ?: R.drawable.main_duck) { uri ->
                        selectedImageUri = uri
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "그룹 사진 선택",
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
                    onValueChange = { name = it },
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
                    onValueChange = { desc = it },
                    placeholder = "그룹에 대한 설명을 작성해주세요 (선택)",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
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
                if (!maxValid && maxMemberText.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "1~6 사이의 숫자만 입력해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9534F)
                    )
                }
            }

            // 공유 데이터
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
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        ToggleRow("운동", shareExercise, { shareExercise = it }, true, true)
                        Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        ToggleRow("심박수", shareHr, { shareHr = it }, true, true)
                        Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        ToggleRow("걸음 수", shareSteps, { shareSteps = it }, true, true)

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "선택 항목",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                        ToggleRow("수면", shareSleep, { shareSleep = it }, true, false)
                        ToggleRow("음수량", shareWater, { shareWater = it }, true, false)
                        ToggleRow("혈압", shareBp, { shareBp = it }, true, false)
                        ToggleRow("혈당", shareSugar, { shareSugar = it }, true, false)
                    }
                }

                if (!requiredAllChecked) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "필수 항목(운동, 심박수, 걸음 수)을 모두 켜주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9534F)
                    )
                }
            }

            // 제출 버튼
            item {
                LcBtn(
                    text = "그룹 생성하기",
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    buttonColor = main,
                    buttonTextColor = white,
                    isEnabled = canSubmit,
                    onClick = {
                        val imagePart = selectedImageUri?.toMultipart(context)
                        vm.createGroup(
                            name = name,
                            description = desc,
                            capacity = maxMember ?: 6,
                            isSleepAllowed = shareSleep,
                            isWaterIntakeAllowed = shareWater,
                            isBloodPressureAllowed = shareBp,
                            isBloodSugarAllowed = shareSugar,
                            imagePart = imagePart
                        )
                    }
                )
                if (state is CareGroupCreateViewModel.UiState.Loading) {
                    LoadingOverlay()
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = black
    )
}