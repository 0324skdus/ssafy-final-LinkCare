package com.a307.linkcare.common.component.atoms

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.avatarTest.AvatarAnimator
import com.a307.linkcare.avatarTest.AvatarSelection
import com.a307.linkcare.avatarTest.Species
import com.a307.linkcare.R
import com.a307.linkcare.common.util.transformation.CropTransparentTransformation
import com.a307.linkcare.common.util.loader.painterResourceCropped
@Composable
fun Avatar(
    @DrawableRes imageRes: Int = R.drawable.char_bear_1,
    avatarUrl: String?,
    backgroundUrl: String?,
    petName: String?,
    bubbleText: String,
    bubbleVisible: Boolean,
    onBubbleVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isExercising: Boolean = false
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0x11000000), CircleShape)
            .clickable { onBubbleVisibleChange(!bubbleVisible) },
        contentAlignment = Alignment.Center
    ) {
        // 배경
        if (!backgroundUrl.isNullOrBlank()) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.background_1),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // 캐릭터 (운동 중이면 스프라이트 애니메이션)
        if (isExercising && !avatarUrl.isNullOrBlank()) {
            // URL에서 species 추출 (예: ".../bear/..." → BEAR)
            val species = remember(avatarUrl) {
                when {
                    avatarUrl.contains("bear", ignoreCase = true) -> Species.BEAR
                    avatarUrl.contains("duck", ignoreCase = true) -> Species.DUCK
                    else -> Species.BEAR // 기본값
                }
            }

            AvatarAnimator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-15).dp)
                    .graphicsLayer(
                        scaleX = 2.7f,
                        scaleY = 2.7f
                    ),
                selection = AvatarSelection(
                    species = species,
                    variant = 1, // 기본값
                    hatId = null,
                    glassesId = null,
                    animation = "walk" // 운동 중 → 걷기 애니메이션
                ),
                fps = 12
            )
        } else if (!avatarUrl.isNullOrBlank()) {
            // 운동 안 할 때는 정적 이미지
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .transformations(CropTransparentTransformation())
                    .size(coil.size.Size.ORIGINAL)
                    .build(),
                contentDescription = petName,
                modifier = Modifier
                    .size(35.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
                onError = { error ->
                    Log.e("Avatar", "image load error: $avatarUrl", error.result.throwable)
                }
            )
        } else {
            // 기본 이미지
            Image(
                painter = painterResourceCropped(resId = imageRes),
                contentDescription = null,
                modifier = Modifier.size(35.dp)
            )
        }

        // 말풍선 (그대로)
        if (bubbleVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -60),
                properties = PopupProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = { onBubbleVisibleChange(false) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = bubbleText,
                            color = Color.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(y = (-3).dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }
            }
        }
    }
}
