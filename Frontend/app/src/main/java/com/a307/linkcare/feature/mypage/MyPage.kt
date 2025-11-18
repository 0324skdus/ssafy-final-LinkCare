@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.CropTransparentTransformation
import com.a307.linkcare.feature.auth.data.RetrofitClient
import com.a307.linkcare.feature.auth.data.TokenStore
import kotlinx.coroutines.launch

/* ---------- 앱 팔레트 ---------- */
private val cardStroke = Color(0xFFE5E5EA)
private val coinGold = Color(0xFFFFB800)
private val shadowColor = Color(0x1A000000)

/* ---------- 공개 API ---------- */

data class MyPageUiState(
    val nickname: String,
    val coinLabel: String,
    val groupCountLabel: String,
    val avatarUrl: String?,
    val avatarBgUrl: String? = null
)

@Composable
fun MyPage(
    state: MyPageUiState,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onDecorateClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val performLogout: () -> Unit = {
        scope.launch {
            try {
                val access = TokenStore(context).getAccess()
                if (access == null) {
                    Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val res = RetrofitClient.authApi.logout("Bearer $access")

                if (res.isSuccessful) {
                    TokenStore(context).clear()
                    Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

                    onLogout()
                } else {
                    Toast.makeText(context, "로그아웃 실패", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "서버 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(white)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // 프로필 카드
            ProfileCard(
                state = state,
                onEditClick = onEditClick,
                onLogoutClick = performLogout,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // 소속 그룹 버튼
            Surface(
                onClick = onGroupsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = main.copy(alpha = 0.25f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = main
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    main,
                                    main.copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.groupCountLabel,
                        color = white,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 하단 카드 2열
            MenuRow(
                onStoreClick = onStoreClick,
                onDecorateClick = onDecorateClick
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

/* ---------- 내부 구성요소 ---------- */
@Composable
private fun ProfileCard(
    state: MyPageUiState,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val imageSize = with(density) { 120.dp.toPx().toInt() }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = shadowColor
            ),
        color = white,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                // 상단 버튼 영역
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onLogoutClick,
                        modifier = Modifier.height(24.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFF5F5F5),
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFF666666)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("로그아웃", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
                        }
                    }
                }

                // 본문 영역
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 아바타 + 배경
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 배경 원형 이미지
                        AsyncImage(
                            model = state.avatarBgUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // 아바타 이미지
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(state.avatarUrl)
                                .crossfade(true)
                                .size(imageSize)
                                .transformations(CropTransparentTransformation())
                                .build(),
                            contentDescription = "avatar",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )

                        // 편집 버튼 (우측 하단, 배경에 살짝 걸치게)
                        Surface(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .align(Alignment.BottomEnd),
                            shape = CircleShape,
                            color = white,
                            shadowElevation = 4.dp,
                            tonalElevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "편집",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF666666)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.nickname,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = black
                        )
                        Spacer(Modifier.height(8.dp))

                        // 코인 표시
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.coin),
                                contentDescription = "coin",
                                modifier = Modifier.size(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                state.coinLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    onStoreClick: () -> Unit,
    onDecorateClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        MenuButtonCard(
            title = "상점",
            icon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(24.dp)) },
            modifier = Modifier.weight(1f),
            onClick = onStoreClick,
            iconColor = Color(0xFF4CAF50)
        )
        Spacer(Modifier.width(16.dp))
        MenuButtonCard(
            title = "꾸미기",
            icon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(24.dp)) },
            modifier = Modifier.weight(1f),
            onClick = onDecorateClick,
            iconColor = Color(0xFFFF9800)
        )
    }
}

@Composable
private fun MenuButtonCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    iconColor: Color = main,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = shadowColor
            ),
        color = white,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }

            Spacer(Modifier.width(14.dp))
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = black
            )
        }
    }
}