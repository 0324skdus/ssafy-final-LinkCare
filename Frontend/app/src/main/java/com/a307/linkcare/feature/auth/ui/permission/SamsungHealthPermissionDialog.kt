package com.a307.linkcare.feature.auth.ui.permission

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.R

@Composable
fun SamsungHealthPermissionDialog(
    onDismiss: () -> Unit,
    onConnectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .noRippleClickable { /* 아무것도 안함. 이벤트 소비 */ },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목
            Text(
                text = "LinkCare를 시작하려면",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Text(
                text = "권한이 필요해요",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(Modifier.height(20.dp))

            // 로고 3개
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "LinkCare",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(20.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "arrow",
                    tint = main,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.samsung_health_72x72),
                    contentDescription = "Samsung Health",
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 안내 문구
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append("LinkCare")
                    }
                    append("는 ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append("Samsung Health")
                    }
                    append("와 연동됩니다.\n")
                    append("일부 건강 관련 정보가 Samsung Health로부터 제공됩니다.\n")
                    append("지금 Samsung Health에 연결해 보세요!")
                },
                color = Color(0xFF333333),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            // 버튼
            LcBtn(
                text = "Samsung Health와 연결하기",
                buttonColor = main,
                buttonTextColor = white,
                onClick = onConnectClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    }