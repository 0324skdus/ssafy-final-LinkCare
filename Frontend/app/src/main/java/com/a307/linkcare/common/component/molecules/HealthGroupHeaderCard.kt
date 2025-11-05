package com.a307.linkcare.common.component.molecules;

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.component.atoms.Avatar
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.feature.healthgroup.Member
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.point
import com.a307.linkcare.common.theme.white

@Composable
fun HealthGroupHeaderCard(
        progress: Float,
        @DrawableRes leaderAvatarRes: Int,
        @DrawableRes bgImgRes: Int
) {
    Card(
            modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
    ) {
        // 배경 하늘 이미지
        Box(
                Modifier
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                    painter = painterResource(bgImgRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )

            // 진행바 + 눈금 + 코인
            BoxWithConstraints(
                    modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
            ) {
                val barHeight = 10.dp
                val coinSize = 28.dp
                val barRadius = 6.dp

                // 바 트랙
                Box(
                        Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(barRadius))
                                .background(Color(0x66FFFFFF))
                )

                // 채워진 바
                Box(
                        Modifier
                                .fillMaxWidth(progress)
                                .height(barHeight)
                                .clip(RoundedCornerShape(barRadius))
                                .background(white)
                )

                // 눈금 (0~100, 10% 간격)
                Canvas(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.BottomCenter)
                ) {
                    val w = size.width
                    val baseY = size.height - 2.dp.toPx()
                    val tickH = 6.dp.toPx()
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        strokeWidth = 2.dp.toPx()
                        isAntiAlias = true
                    }
                    // 11개(0..10)
                    for (i in 0..10) {
                        val x = w * (i / 10f)
                        drawLine(
                                color = Color.White.copy(alpha = if (i % 5 == 0) 0.95f else 0.8f),
                        start = Offset(x, baseY - tickH),
                                end = Offset(x, baseY + 2.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // 코인 위치 (바 위쪽 살짝)
                val coinOffsetX = with(LocalDensity.current) {
                    val total = maxWidth.toPx()
                    val c = coinSize.toPx()
                    ((total - c) * progress).coerceIn(0f, total - c)
                }

                Image(
                        painter = painterResource(R.drawable.coin),
                        contentDescription = null,
                        modifier = Modifier
                                .offset { IntOffset(coinOffsetX.toInt(), (-8).dp.roundToPx()) }
                        .size(coinSize)
                        .align(Alignment.CenterStart)
                )
            }

            // 모임장 캐릭터(우상단). 100%면 코인 “들고 있는” 오버레이 추가
            Box(
                    Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                    contentAlignment = Alignment.Center
            ) {
                Image(
                        painter = painterResource(leaderAvatarRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .offset(x = 10.dp, y = -20.dp)
                )
            }
        }
    }
}
