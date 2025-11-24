package com.a307.linkcare.common.component.molecules.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.component.atoms.*


@Composable
fun BaselineInputs(
    kcalText: String,
    minutesText: String,
    stepsText: String,
    kmText: String,
    onKcalChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onStepsChange: (String) -> Unit,
    onKmChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 안내 문구
        Text(
            text = "앞으로 목표를 생성할 때 참고할 데이터입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E8E93)
        )
        Spacer(Modifier.height(2.dp))

        // 1행: kcal | min
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            BaselineChipField(
                value = kcalText,
                onValueChange = onKcalChange,
                unit = "kcal",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                allowDecimal = false
            )
            BaselineChipField(
                value = minutesText,
                onValueChange = onMinutesChange,
                unit = "min",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                allowDecimal = false
            )
        }

        // 2행: steps | km
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            BaselineChipField(
                value = stepsText,
                onValueChange = onStepsChange,
                unit = "steps",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                allowDecimal = false
            )
            BaselineChipField(
                value = kmText,
                onValueChange = onKmChange,
                unit = "km",
                modifier = Modifier.weight(1f),
                enabled = enabled,
                allowDecimal = true  // 소수 허용
            )
        }
    }
}

/** pill 모양 인풋 + 우측 단위 라벨 */
@Composable
private fun BaselineChipField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    allowDecimal: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(44.dp)
            .background(Color.Transparent)
    ) {
        // 숫자 입력
        LcInputField(
            value = value,
            onValueChange = { s ->
                if (!enabled) return@LcInputField
                if (s.isEmpty()) {
                    onValueChange(s)
                } else {
                    // 정수/실수 제한
                    val ok = if (allowDecimal) {
                        // "12", "12.", "12.3" 허용 (앞자리에 0 여러개 방지까진 필요시 추가)
                        s.all { it.isDigit() || it == '.' } && s.count { it == '.' } <= 1
                    } else {
                        s.all { it.isDigit() }
                    }
                    if (ok && s.length <= 6) onValueChange(s)
                }
            },
            placeholder = "",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp)),
            singleLine = true,
            backgroundColor = Color(0xFFF5F6FA),
        )
        Spacer(Modifier.width(8.dp))
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
    }
}
