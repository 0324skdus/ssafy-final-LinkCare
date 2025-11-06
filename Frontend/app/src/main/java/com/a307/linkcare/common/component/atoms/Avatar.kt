package com.a307.linkcare.common.component.atoms

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.R
import com.a307.linkcare.common.util.painterResourceCropped


@Composable
fun Avatar(
    @DrawableRes imageRes: Int,
    bubbleText: String,
    modifier: Modifier = Modifier,
    bubbleVisible: Boolean,
    onBubbleVisibleChange: (Boolean) -> Unit,
    @DrawableRes bgImgRes: Int,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(bgImgRes),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // --- 말풍선 (Popup 오버레이) ---
        if (bubbleVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -60), // 아바타 위로 띄우기
                properties = PopupProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = { onBubbleVisibleChange(false) }
            ) {
                // 말풍선 본체
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    // 말풍선 꼬리
                    Canvas(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(y = (-3).dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)                     // 왼쪽 위
                            lineTo(size.width, 0f)             // 오른쪽 위
                            lineTo(size.width / 2f, size.height) // 아래쪽 중앙 (꼭짓점)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }
            }
        }

        // --- 캐릭터 아바타 ---
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0x11000000), CircleShape)
                .clickable { onBubbleVisibleChange(!bubbleVisible) },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResourceCropped(resId = imageRes),
                contentDescription = null,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}
