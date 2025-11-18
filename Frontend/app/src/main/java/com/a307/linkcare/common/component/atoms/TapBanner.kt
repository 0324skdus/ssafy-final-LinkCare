package com.a307.linkcare.common.component.atoms

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable // Added this import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.white

/**
 * 콕찌르기 알림 배너 (atoms)
 *
 * - 오른쪽에 손 아이콘 + 닫기(X) 버튼
 * - 이름(예: "지우님") 강조 굵게
 * - onDismiss 호출 시 사라짐 (호이스팅)
 * - focused=true면 파란 외곽선(선택/포커스 상태)
 */
@Composable
fun NudgeBanner(
    message: String,
    nameToBold: String? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFFF6B35),
    contentColor: Color = white,
    focused: Boolean = false,
    onDismiss: () -> Unit,
    @DrawableRes handIconRes: Int = R.drawable.tap,
    onIconClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier.clip(shape),
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 메시지
            Text(
                text = buildAnnotatedString {
                    if (!nameToBold.isNullOrBlank() && message.startsWith(nameToBold)) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(nameToBold)
                        }
                        append(message.removePrefix(nameToBold))
                    } else {
                        append(message)
                    }
                },
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )

            // 손 아이콘
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(handIconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .then(if (onIconClick != null) Modifier.clickable { onIconClick() } else Modifier),
                tint = Color.Unspecified
            )

            // X 닫기
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(contentColor = white)
            ) {
                Icon(Icons.Default.Close, contentDescription = "닫기")
            }
        }
    }
}

/**
 * 여러 개 알림을 자연스럽게 쌓아 보여주는 호스트.
 * - items: 식별 가능한 리스트
 * - 각 아이템마다 onDismiss 시 remove 호출
 */
@Composable
fun <T> NudgeHost(
    items: List<T>,
    key: (T) -> Any,
    message: (T) -> String,
    nameToBold: (T) -> String?,
    focused: (T) -> Boolean = { false },
    onDismiss: (T) -> Unit,
    modifier: Modifier = Modifier,
    onIconClick: ((T) -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(animationSpec = tween(180)) { it } + fadeIn(tween(180)),
                exit  = slideOutVertically(animationSpec = tween(160)) { it } + fadeOut(tween(160))
            ) {
                NudgeBanner(
                    message = message(item),
                    nameToBold = nameToBold(item),
                    focused = focused(item),
                    onDismiss = { onDismiss(item) },
                    onIconClick = onIconClick?.let { { it(item) } }
                )
            }
        }
    }
}
