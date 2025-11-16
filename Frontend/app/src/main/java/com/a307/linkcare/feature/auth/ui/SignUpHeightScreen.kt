@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.atoms.LcInputField
import com.a307.linkcare.common.component.molecules.PagerDots
import com.a307.linkcare.common.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignupHeightScreen(
    pagerState: PagerState,
    pageCount: Int,
    onSubmit: (Pair<Float, Float>) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }

    fun parseFloatSafe(s: String) = s.toFloatOrNull()

    val height = parseFloatSafe(heightText)
    val weight = parseFloatSafe(weightText)

    val isHeightValid = height != null && height in 100.0..250.0
    val isWeightValid = weight != null && weight in 30.0..250.0
    val canSubmit = isHeightValid && isWeightValid

    val duckAspect = 462f / 281f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
    ) {
        val arcOffsetUp = 24.dp
        val duckHeight = maxWidth / duckAspect
        val arcHeight = duckHeight + arcOffsetUp
        val contentHeight = (maxHeight - arcHeight).coerceAtLeast(0.dp)

        // 하단 파란 아크
        BottomBlueArc(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(arcHeight)
        )

        // 본문
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(contentHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "싸피님에 대해서",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "더 자세히 알고싶어요",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "* 스마트한 건강관리를 위해 정확한 정보를 입력해주세요",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // 키(cm)
                UnitInputField(
                    value = heightText,
                    onValueChange = { s ->
                        heightText = s.filter { it.isDigit() }.take(3)
                    },
                    placeholder = "키를 입력하세요",
                    unit = "cm"
                )

                Spacer(Modifier.height(12.dp))

                // 몸무게(kg)
                UnitInputField(
                    value = weightText,
                    onValueChange = { s ->
                        weightText = s.filter { it.isDigit() }.take(3)
                    },
                    placeholder = "몸무게를 입력하세요",
                    unit = "kg"
                )

                Spacer(Modifier.height(20.dp))

                // 확인 버튼
                LcBtn(
                    text = "확인",
                    modifier = Modifier.fillMaxWidth(),
                    buttonColor = if (canSubmit) main else unActiveBtn,
                    buttonTextColor = if (canSubmit) white else unActiveField,
                    isEnabled = canSubmit,
                    onClick = {
                        onSubmit(height!! to weight!!)
                    }
                )
            }
        }

        // 오리 이미지
        Image(
            painter = painterResource(id = R.drawable.main_duck),
            contentDescription = "main_duck",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(duckHeight),
            contentScale = ContentScale.Fit
        )

        // 상단 페이지 도트
        PagerDots(
            total = pageCount,
            current = pagerState.currentPage,
            onDotClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )
    }
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
        // 입력 필드
        LcInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .matchParentSize()
                .padding(end = 52.dp)
        )

        // 우측 단위 배지
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
private fun BottomBlueArc(
    modifier: Modifier = Modifier,
    color: Color = main
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 195.dp, topEnd = 195.dp))
            .background(color)
    )
}
