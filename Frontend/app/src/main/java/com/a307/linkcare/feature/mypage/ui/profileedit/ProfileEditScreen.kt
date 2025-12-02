package com.a307.linkcare.feature.mypage.ui.profileedit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.atoms.LcInputField
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.mypage.data.model.dto.Profile
import com.a307.linkcare.feature.mypage.data.model.response.MyProfileResponse
import java.util.Calendar

enum class Gender { MALE, FEMALE }

fun MyProfileResponse.toProfile(): Profile =
    Profile(
        name = name,
        gender = if (gender == "남") Gender.MALE else Gender.FEMALE,
        birthDate = birth,
        heightCm = height.toInt(),
        weightKg = weight.toInt(),
        petName = petName
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    initial: Profile,
    onSave: (Profile) -> Unit,
    onBack: () -> Unit,
    @DrawableRes illustrationRes: Int = R.drawable.ic_launcher_foreground
) {
    // ----- 상태 (name은 read-only 이라 상태 X) -----
    var gender by rememberSaveable { mutableStateOf(initial.gender) }
    var height by rememberSaveable { mutableStateOf(initial.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf(initial.weightKg.toString()) }
    var petName by rememberSaveable { mutableStateOf(initial.petName ?: "") }

    // 생년월일 파싱
    val (initY, initM, initD) = remember(initial.birthDate) {
        val p = initial.birthDate.split("-")
        Triple(p.getOrNull(0) ?: "", p.getOrNull(1) ?: "", p.getOrNull(2) ?: "")
    }
    var year by rememberSaveable { mutableStateOf(initY) }
    var month by rememberSaveable { mutableStateOf(initM) }
    var day by rememberSaveable { mutableStateOf(initD) }

    // ----- 유효성 검사 -----
    val heightInt = height.toIntOrNull()
    val weightInt = weight.toIntOrNull()

    val validBirth = isValidDate(year, month, day)
    val validHeight = heightInt != null && heightInt in 50..250
    val validWeight = weightInt != null && weightInt in 20..300

    val newBirthDate = "${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}"

    val isChanged =
        gender != initial.gender ||
                newBirthDate != initial.birthDate ||
                heightInt != initial.heightCm ||
                weightInt != initial.weightKg ||
                petName != initial.petName

    val isValid = validBirth && validHeight && validWeight

    Scaffold(
        containerColor = white,
        contentColor = black,
        topBar = {
        },
        bottomBar = {
            Box(Modifier.padding(16.dp)) {
                LcBtn(
                    text = "저장하기",
                    onClick = {
                        onSave(
                            Profile(
                                name = initial.name,
                                gender = gender,
                                birthDate = newBirthDate,
                                heightCm = heightInt ?: initial.heightCm,
                                weightKg = weightInt ?: initial.weightKg,
                                petName = petName
                            )
                        )
                    },
                    isEnabled = isValid && isChanged,
                    modifier = Modifier.fillMaxWidth(),
                    buttonColor = main,
                    buttonTextColor = white
                )
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ----- 이름 (조회 전용) -----
                Text("이름", color = black, style = MaterialTheme.typography.labelLarge)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = initial.name,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = black,
                        fontSize = 14.sp
                    )
                }

                // ----- 펫 이름 -----
                Text("캐릭터 이름", color = black, style = MaterialTheme.typography.labelLarge)
                LcInputField(
                    value = petName,
                    onValueChange = { petName = it },
                    placeholder = "캐릭터 이름 입력",
                    modifier = Modifier.fillMaxWidth()
                )

                // ----- 성별 -----
                Text("성별", color = black, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderChip(
                        text = "남성",
                        selected = gender == Gender.MALE,
                        onClick = { gender = Gender.MALE }
                    )
                    GenderChip(
                        text = "여성",
                        selected = gender == Gender.FEMALE,
                        onClick = { gender = Gender.FEMALE }
                    )
                }

                // ----- 생년월일 -----
                Text("생년월일", color = black, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = year,
                        onValueChange = { s -> year = s.filter { it.isDigit() }.take(4) },
                        placeholder = "1999"
                    )
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = month,
                        onValueChange = { s -> month = s.filter { it.isDigit() }.take(2) },
                        placeholder = "02"
                    )
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = day,
                        onValueChange = { s -> day = s.filter { it.isDigit() }.take(2) },
                        placeholder = "28"
                    )
                }
                if (!validBirth) AssistiveText("올바른 생년월일(yyyy-MM-dd)을 입력하세요.")

                // ----- 키 -----
                Text("키 (cm)", color = black, style = MaterialTheme.typography.labelLarge)
                UnitInputField(
                    value = height,
                    onValueChange = { input -> height = input.filter { it.isDigit() }.take(3) },
                    placeholder = "예: 168",
                    unit = "cm"
                )
                if (!validHeight) AssistiveText("키는 50~250cm 사이로 입력하세요.")

                // ----- 몸무게 -----
                Text("몸무게 (kg)", color = black, style = MaterialTheme.typography.labelLarge)
                UnitInputField(
                    value = weight,
                    onValueChange = { input -> weight = input.filter { it.isDigit() }.take(3) },
                    placeholder = "예: 60",
                    unit = "kg"
                )
                if (!validWeight) AssistiveText("몸무게는 20~300kg 사이로 입력하세요.")

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun GenderChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, color = if (selected) white else black) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = white,
            labelColor = black,
            selectedContainerColor = main,
            selectedLabelColor = white,
            iconColor = black,
            selectedLeadingIconColor = white
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) main else unActiveBtn
        )
    )
}

@Composable
private fun AssistiveText(msg: String) {
    Text(
        text = msg,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFD32F2F),
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun UnitInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    unit: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        LcInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .matchParentSize()
                .padding(end = 52.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .height(28.dp)
                .widthIn(min = 36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF3F4F6))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun BirthField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(modifier = modifier.height(44.dp)) {
        LcInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun isValidDate(y: String, m: String, d: String): Boolean {
    val yy = y.toIntOrNull() ?: return false
    val mm = m.toIntOrNull() ?: return false
    val dd = d.toIntOrNull() ?: return false
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    if (yy !in 1900..nowYear) return false
    if (mm !in 1..12) return false
    val maxDay = when (mm) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((yy % 400 == 0) || (yy % 4 == 0 && yy % 100 != 0)) 29 else 28
        else -> 31
    }
    return dd in 1..maxDay
}
