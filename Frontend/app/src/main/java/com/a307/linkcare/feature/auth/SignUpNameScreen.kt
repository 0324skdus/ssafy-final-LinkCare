@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.a307.linkcare.common.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignupNameScreen(
    pagerState: PagerState,
    pageCount: Int,
    onSubmit: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var nameCheck by rememberSaveable { mutableStateOf("") }
    val canSubmit = nameCheck.isNotBlank()

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
                    text = "당신을 편하게 부르고 싶지만",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = black,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "이름",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = main
                    )
                    Text(
                        text = "을 알지 못합니다",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = black
                    )
                }

                Spacer(Modifier.height(35.dp))

                LcInputField(
                    value = nameCheck,
                    onValueChange = { nameCheck = it },
                    placeholder = "이름을 입력하세요"
                )

                Spacer(Modifier.height(20.dp))

                LcBtn(
                    text = "확인",
                    modifier = Modifier.fillMaxWidth(),
                    buttonColor = if (canSubmit) main else unActiveBtn,
                    buttonTextColor = if (canSubmit) white else unActiveField,
                    isEnabled = canSubmit,
                    onClick = { onSubmit(nameCheck) }
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
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (selected) 20.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) main else Color.LightGray)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                )
                if (index != pageCount - 1) Spacer(Modifier.width(8.dp))
            }
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
