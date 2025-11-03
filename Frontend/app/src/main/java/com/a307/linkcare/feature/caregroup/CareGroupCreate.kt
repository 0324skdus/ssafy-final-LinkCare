package com.a307.linkcare.feature.caregroup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.feature.caregroup.domain.ShareOptions
import com.a307.linkcare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGroupCreate(
    navController: NavHostController = rememberNavController(),
    currentDestination: NavDestination? = navController.currentBackStackEntryAsState().value?.destination,
    onClickCreate: (
        name: String,
        description: String,
        maxMember: Int,
        share: ShareOptions
    ) -> Unit = { _, _, _, _ -> },
    onSelectImage: () -> Unit = {},
    @DrawableRes avatarRes: Int? = null
) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                HeaderBack(
                    title = "케어 그룹 생성",
                    onBackClick = { navController.popBackStack() }
                )
                Divider(color = unActiveField, thickness = 1.dp)
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 아바타
            // 상단 아바타 영역 교체
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GroupAvatarPicker(
                        defaultRes = R.drawable.empty_group
                    ) { uri ->
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
                Column {
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
            }

            // 그룹 설명
            item {
                Column {
                    SectionTitle("그룹 설명")
                    Spacer(Modifier.height(8.dp))
                    LcInputField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = "그룹에 대한 설명을 작성해주세요 (선택)",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                        singleLine = false,
                        backgroundColor = Color(0xFFF5F6FA)
                    )
                }
            }

            // 최대 인원
            item {
                Column {
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
                                // 빈값 또는 한 자리 숫자(1~6)만 허용
                                if (s.isEmpty()) {
                                    maxMemberText = s
                                } else if (s.length <= 1 && s.all { it.isDigit() } && s.toInt() in 1..6) {
                                    maxMemberText = s
                                }
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
            }

            // 공유 데이터 설정 (필수)
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("공유 데이터 설정")
                        Spacer(Modifier.width(8.dp))
                        AssistChip(text = "필수 항목 포함")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "그룹원들과 공유할 건강 데이터를 선택해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FA)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            // 필수 3종
                            ToggleRow(
                                label = "운동",
                                checked = shareExercise,
                                onChange = { shareExercise = it },
                                enabled = true,
                                isRequired = true
                            )
                            Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            ToggleRow(
                                label = "심박수",
                                checked = shareHr,
                                onChange = { shareHr = it },
                                enabled = true,
                                isRequired = true
                            )
                            Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            ToggleRow(
                                label = "걸음 수",
                                checked = shareSteps,
                                onChange = { shareSteps = it },
                                enabled = true,
                                isRequired = true
                            )

                            // 선택 항목(필수 배지 X)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "선택 항목",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93),
                                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                            )
                            ToggleRow(
                                label = "수면",
                                checked = shareSleep,
                                onChange = { shareSleep = it },
                                enabled = true,
                                isRequired = false
                            )
                            ToggleRow(
                                label = "음수량",
                                checked = shareWater,
                                onChange = { shareWater = it },
                                enabled = true,
                                isRequired = false
                            )
                            ToggleRow(
                                label = "혈압",
                                checked = shareBp,
                                onChange = { shareBp = it },
                                enabled = true,
                                isRequired = false
                            )
                            ToggleRow(
                                label = "혈당",
                                checked = shareSugar,
                                onChange = { shareSugar = it },
                                enabled = true,
                                isRequired = false
                            )
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
            }

            // 제출 버튼
            item {
                LcBtn(
                    text = "그룹 생성하기",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    buttonColor = main,
                    buttonTextColor = white,
                    isEnabled = canSubmit,
                    onClick = {
                        val max = maxMember ?: 6
                        onClickCreate(
                            name.trim(),
                            desc.trim(),
                            max,
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
                    }
                )
            }
        }
    }
}

/** 소제목 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = black
    )
}

@Composable
private fun AssistChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = main,
        modifier = Modifier
            .background(color = main.copy(alpha = 0.1f), shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}