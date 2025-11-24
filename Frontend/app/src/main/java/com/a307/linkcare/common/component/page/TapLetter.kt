package com.a307.linkcare.common.component.page

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.a307.linkcare.common.util.transformation.CropTransparentTransformation

data class User(
    val name: String,
    @DrawableRes val avatarRes: Int?,   // 기본 이미지
    val avatarUrl: String? = null,      // 서버 캐릭터 이미지
    val backgroundUrl: String? = null,  // 배경
    val petName: String? = null         // 펫 이름
)

data class Suggestion(val id: Int, val text: String)

private val MainBlue = Color(0xFF4A89F6)
private val CardStroke = Color(0x1F000000)
private val AvatarBg = Color(0xFFD5E0F5)

@Composable
fun NudgeLetterCard(
    modifier: Modifier = Modifier,
    sender: User,
    receiver: User,
    suggestions: List<Suggestion>,
    onClose: () -> Unit = {},
    onSend: (suggestion: Suggestion?, customText: String) -> Unit = { _, _ -> }
) {
    var selectedId by remember { mutableStateOf(suggestions.firstOrNull()?.id ?: -1) }
    var customText by remember { mutableStateOf("") }
    val isCustomSelected = selectedId == -1
    val canSend = if (isCustomSelected) customText.isNotBlank() else selectedId != -1

    Box(modifier = modifier.wrapContentSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = 700.dp),  // 최대 높이 제한 (스크롤 가능)
            color = Color.White,
            shape = RoundedCornerShape(26.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // 헤더 (X 버튼)
                Box(Modifier.fillMaxWidth().height(56.dp)) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = Color(0xFF666666))
                    }
                }

                // 스크롤 가능 영역
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        // 프로필
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(sender, Modifier)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Email,
                                    null,
                                    Modifier.size(20.dp),
                                    tint = MainBlue
                                )
                                Spacer(Modifier.height(4.dp))
                                Icon(
                                    Icons.Outlined.ArrowForward,
                                    null,
                                    Modifier.size(32.dp),
                                    tint = MainBlue
                                )
                            }
                            ProfileAvatar(receiver, Modifier)
                        }

                        Spacer(Modifier.height(24.dp))

                        // 옵션 목록 (길어져도 스크롤됨)
                        suggestions.forEach { s ->
                            OptionCard(
                                text = s.text,
                                selected = selectedId == s.id,
                                onClick = { selectedId = s.id }
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        OptionCard(
                            text = "직접 편지 작성",
                            selected = isCustomSelected,
                            onClick = { selectedId = -1 }
                        )

                        if (isCustomSelected) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedCard(
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 260.dp), // <-- 스크롤 고려
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(Modifier.fillMaxSize().padding(12.dp)) {
                                    val scroll = rememberScrollState()
                                    BasicTextField(
                                        value = customText,
                                        onValueChange = { customText = it },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scroll), // <-- 긴 편지 스크롤 가능
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (customText.isEmpty()) {
                                                Text(
                                                    "직접 편지를 작성해주세요",
                                                    color = Color(0xFFB4B2B2),
                                                    fontSize = 16.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }

                // 하단 고정 버튼
                Button(
                    onClick = {
                        val selected = suggestions.firstOrNull { it.id == selectedId }
                        onSend(selected, if (isCustomSelected) customText.trim() else "")
                    },
                    enabled = canSend,
                    colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .width(275.dp)
                        .height(48.dp)
                ) {
                    Text("보내기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(user: User, modifier: Modifier) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0x11000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 배경
            if (!user.backgroundUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.backgroundUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 캐릭터
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.avatarUrl)
                        .transformations(CropTransparentTransformation())
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = user.petName,
                    modifier = Modifier
                        .size(35.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        user.petName?.let { Text(it, fontSize = 14.sp, color = Color.Black) }
        Text("(${user.name})", fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun OptionCard(text: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MainBlue.copy(alpha = 0.5f) else CardStroke
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = if (selected) 1.5.dp else 1.dp,
            brush = SolidColor(borderColor)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                fontSize = 15.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                lineHeight = 20.sp
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MainBlue)
                    )
                }
            }
        }
    }
}
