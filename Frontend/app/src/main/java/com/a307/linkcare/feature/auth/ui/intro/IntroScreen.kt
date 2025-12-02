package com.a307.linkcare.feature.auth.ui.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*

@Composable
fun IntroScreen(
    onDone: () -> Unit = {}
) {
    val duckAspect = 462f / 281f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(main)
    ) {
        val duckHeight = maxWidth / duckAspect
        val contentHeight = (maxHeight - duckHeight).coerceAtLeast(0.dp)

        // 콘텐츠 영역
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(contentHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // logo
                Text(
                    text = "LinkCare",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = white,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // 문구
                Text(
                    text = "만나게 되어 반가워요",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = white
                )
            }
        }

        // 아래 오리 이미지
        Image(
            painter = painterResource(id = R.drawable.main_duck),
            contentDescription = "main_duck",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(duckHeight),
            contentScale = ContentScale.Fit
        )
    }
}
