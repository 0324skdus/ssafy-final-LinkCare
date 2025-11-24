@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.molecules.header.PagerDots
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.network.store.TokenStore
import kotlinx.coroutines.launch

private enum class Gender { MALE, FEMALE }

@Composable
fun SignupGenderScreen(
    pagerState: PagerState,
    pageCount: Int,
    onSubmit: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedGender by rememberSaveable { mutableStateOf<Gender?>(null) }
    val canSubmit = selectedGender != null
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val userName = remember { tokenStore.getName().orEmpty() }

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
                // 제목
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (userName.isNotBlank()) userName + "님" else "당신"}의",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = black
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "성별",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = main
                    )
                    Text(
                        text = "은 무엇인가요?",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = black
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "* 스마트한 건강관리를 위해 정확한 정보를 입력해주세요",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // 성별 선택 칩
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GenderChip(
                        text = "남성",
                        selected = selectedGender == Gender.MALE,
                        onClick = { selectedGender = Gender.MALE }
                    )
                    GenderChip(
                        text = "여성",
                        selected = selectedGender == Gender.FEMALE,
                        onClick = { selectedGender = Gender.FEMALE }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 확인 버튼
                LcBtn(
                    text = "확인",
                    modifier = Modifier.fillMaxWidth(),
                    buttonColor = if (canSubmit) main else unActiveBtn,
                    buttonTextColor = if (canSubmit) white else unActiveField,
                    isEnabled = canSubmit,
                    onClick = {
                        onSubmit(
                            when (selectedGender) {
                                Gender.MALE -> "남"
                                Gender.FEMALE -> "여"
                                null -> ""
                            }
                        )
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

        // 상단 dots
        PagerDots(
            total = pageCount,
            current = pagerState.currentPage,
            onDotClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )
    }
}

@Composable
private fun RowScope.GenderChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) main else Color(0xFFE5E7EB)
    val bg = if (selected) Color(0x1A4A89F6) else white
    val txt = if (selected) main else black

    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = txt
        )
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
