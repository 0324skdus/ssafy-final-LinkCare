@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.a307.linkcare.common.component.atoms.LcInputField
import com.a307.linkcare.common.component.molecules.header.PagerDots
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.network.store.TokenStore
import kotlinx.coroutines.launch

@Composable
fun SignupBirthScreen(
    pagerState: PagerState,
    pageCount: Int,
    onSubmit: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val userName = remember { tokenStore.getName().orEmpty() }

    // 입력 상태
    var year by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }

    fun isValidDate(y: String, m: String, d: String): Boolean {
        val yy = y.toIntOrNull() ?: return false
        val mm = m.toIntOrNull() ?: return false
        val dd = d.toIntOrNull() ?: return false
        if (yy !in 1900..2100) return false
        if (mm !in 1..12) return false
        val maxDay = when (mm) {
            1,3,5,7,8,10,12 -> 31
            4,6,9,11 -> 30
            2 -> if ((yy % 400 == 0) || (yy % 4 == 0 && yy % 100 != 0)) 29 else 28
            else -> 30
        }
        return dd in 1..maxDay
    }
    val canSubmit = isValidDate(year, month, day)

    // 오리 이미지 비율
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (userName.isNotBlank()) userName + "님" else "당신"}이",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = black
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "태어난 연도",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = main
                        )
                        Text(
                            text = "를 알려주세요",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = black
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "* 스마트한 건강관리를 위해 정확한 정보를 입력해주세요",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // 연/월/일 입력칩
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = year,
                        onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4) },
                        placeholder = "1999"
                    )
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = month,
                        onValueChange = { s ->
                            val onlyNum = s.filter { it.isDigit() }.take(2)
                            month = onlyNum
                        },
                        placeholder = "02"
                    )
                    BirthField(
                        modifier = Modifier.weight(1f),
                        value = day,
                        onValueChange = { s ->
                            val onlyNum = s.filter { it.isDigit() }.take(2)
                            day = onlyNum
                        },
                        placeholder = "28"
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
                        onSubmit(String.format("%s-%s-%s", year, month.padStart(2, '0'), day.padStart(2, '0')))
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
private fun BirthField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = modifier
            .height(44.dp)
    ) {
        LcInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.fillMaxSize()
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
