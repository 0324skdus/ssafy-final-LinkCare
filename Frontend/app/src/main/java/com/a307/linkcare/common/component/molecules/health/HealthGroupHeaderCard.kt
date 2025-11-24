package com.a307.linkcare.common.component.molecules.health

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.R
import com.a307.linkcare.avatarTest.AvatarAnimator
import com.a307.linkcare.avatarTest.AvatarSelection
import com.a307.linkcare.avatarTest.Species
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.common.util.transformation.CropTransparentTransformation
import kotlin.math.roundToInt

@Composable
fun HealthGroupHeaderCard(
    progress: Float,
    @DrawableRes leaderAvatarRes: Int,
    @DrawableRes bgImgRes: Int,
    avatarUrl: String?,
    backgroundUrl: String?,
    isLeaderExercising: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x40000000)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // 배경 이미지
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backgroundUrl.takeUnless { it.isNullOrBlank() } ?: bgImgRes)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 그라데이션
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x15000000),
                                Color(0x40000000)
                            )
                        )
                    )
            )

            // 리더 아바타 (운동 중이면 애니메이션)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLeaderExercising && !avatarUrl.isNullOrBlank()) {
                    // URL에서 species 추출
                    val species = remember(avatarUrl) {
                        when {
                            avatarUrl.contains("bear", ignoreCase = true) -> Species.BEAR
                            avatarUrl.contains("duck", ignoreCase = true) -> Species.DUCK
                            else -> Species.BEAR
                        }
                    }

                    AvatarAnimator(
                        modifier = Modifier
                            .offset(x = 70.dp, y = (-40).dp),
                        selection = AvatarSelection(
                            species = species,
                            variant = 1,
                            hatId = null,
                            glassesId = null,
                            animation = "walk"
                        ),
                        fps = 12
                    )
                } else {
                    // 운동 안 할 때는 정적 이미지
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl.takeUnless { it.isNullOrBlank() } ?: leaderAvatarRes)
                            .crossfade(true)
                            .transformations(CropTransparentTransformation())
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .offset(x = (-30).dp, y = (-10).dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 진행바
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                val barHeight = 12.dp
                val coinSize = 32.dp
                val barRadius = 8.dp
                val totalWidthDp = maxWidth

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 진행률 텍스트
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "그룹 목표 달성률",
                            color = white,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${(animatedProgress * 100).roundToInt()}%",
                            color = white,
                            fontSize = 16.sp
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 바 트랙
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(barRadius))
                                .background(Color(0x4DFFFFFF))
                        )

                        // 채워진 바
                        Box(
                            Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                .height(barHeight)
                                .clip(RoundedCornerShape(barRadius))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFF0F0F0)
                                        )
                                    )
                                )
                        )

                        // 눈금
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            val w = size.width
                            val baseY = size.height - 4.dp.toPx()
                            val tickH = 8.dp.toPx()

                            for (i in 0..10) {
                                val x = w * (i / 10f)
                                val alpha = if (i % 5 == 0) 0.9f else 0.6f
                                val width = if (i % 5 == 0) 2.dp.toPx() else 1.5.dp.toPx()

                                drawLine(
                                    color = Color.White.copy(alpha = alpha),
                                    start = Offset(x, baseY - tickH),
                                    end = Offset(x, baseY + 2.dp.toPx()),
                                    strokeWidth = width
                                )
                            }
                        }

                        // 코인 위치
                        val coinOffsetX = with(density) {
                            val total = totalWidthDp.toPx()
                            val c = coinSize.toPx()
                            ((total - c) * animatedProgress.coerceIn(0f, 1f))
                                .coerceIn(0f, total - c)
                        }

                        // 코인 이미지
                        Image(
                            painter = painterResource(R.drawable.coin),
                            contentDescription = null,
                            modifier = Modifier
                                .offset { IntOffset(coinOffsetX.toInt(), (-12).dp.roundToPx()) }
                                .size(coinSize)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}
