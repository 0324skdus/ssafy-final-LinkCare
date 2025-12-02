@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth.ui.intro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.molecules.header.PagerDots
import com.a307.linkcare.common.theme.*
import kotlinx.coroutines.launch

@Composable
fun IntroTourScreen(
    onDone: () -> Unit = {}
) {
    val pages = listOf(
        IntroPage(R.drawable.intro_1, "서로의 건강을 캐릭터로 한눈에 확인하세요", bg = background1),
        IntroPage(R.drawable.intro_2, "웨어러블로 건강 데이터를 실시간 측정해요", bg = main),
        IntroPage(R.drawable.intro_3, "'콕 찌르기'로 응원하고 함께 미션을 달성해요", bg = background1),
        IntroPage(R.drawable.intro_4, "감정으로 연결되는 건강, LinkCare", bg = main)
    )

    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(p.bg)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PagerDots(
                    total = 4,
                    current = pagerState.currentPage,
                    onDotClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
                )

                Spacer(Modifier.height(28.dp))
                Text(
                    text = p.title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (p.bg == main) white else Color(0xFF111111),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = p.imageRes),
                    contentDescription = "intro_$page",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))
                if (page == pages.lastIndex) {
                    LcBtn(
                        text = "메인 화면으로 이동",
                        buttonColor = point,
                        buttonTextColor = white,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        onClick = onDone
                    )
                    Spacer(Modifier.height(15.dp))
                }
            }
        }
    }
}

private data class IntroPage(val imageRes: Int, val title: String, val bg: Color)
