@file:OptIn(ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.painterResourceCropped
import com.a307.linkcare.R
import com.a307.linkcare.feature.auth.data.RetrofitClient
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* ---------- 앱 팔레트 ---------- */
private val cardStroke = Color(0xFFE5E5EA)

/* ---------- 공개 API ---------- */

data class MyPageUiState(
    val nickname: String,
    val coinLabel: String,
    val groupCountLabel: String,
    @DrawableRes val avatarRes: Int,
    @DrawableRes val avatarBgRes: Int? = null
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(white)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // 프로필 카드
            ProfileCard(
                state = state,
                onEditClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // 소속 그룹 버튼
            Button(
                onClick = onGroupsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = main)
            ) {
                Text(state.groupCountLabel, color = white, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // 하단 카드 2열
            MenuRow(
                context = context,
                scope = scope,
                onStoreClick = onStoreClick,
                onDecorateClick = onDecorateClick,
                onLogout = onLogout
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
    onEditClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        color = white,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, cardStroke)
    ) {
        Box(Modifier.fillMaxWidth()) {

            Button(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .height(30.dp),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = main,
                    contentColor = white
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("편집", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // 본문 영역
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아바타 + 배경
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    state.avatarBgRes?.let {
                        Image(
                            painter = painterResource(it),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Image(
                        painter = painterResourceCropped(state.avatarRes),
                        contentDescription = "avatar",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.nickname,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = black
                    )
                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.coin), // <-- coin.png를 프로젝트에 넣고 이름 맞춰줘
                            contentDescription = "coin",
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(state.coinLabel, fontSize = 13.sp, color = Color(0xFF666666))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    context: Context,
    scope: CoroutineScope,
    onStoreClick: () -> Unit,
    onDecorateClick: () -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // 상단: 버튼(카드) 두 개
        Row(Modifier.fillMaxWidth()) {
            MenuButtonCard(
                title = "상점",
                icon = { Icon(Icons.Default.ShoppingCart, null) },
                modifier = Modifier.weight(1f),
                onClick = onStoreClick,
            )
            Spacer(Modifier.width(12.dp))
            MenuButtonCard(
                title = "꾸미기",
                icon = { Icon(Icons.Default.Lock, null) },
                modifier = Modifier.weight(1f),
                onClick = onDecorateClick
            )
        }

        Spacer(Modifier.height(8.dp))

        // 하단
        Row(Modifier.fillMaxWidth()) {
            Text(
                "로그아웃",
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
                    .clickable {
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
                    },
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )


            Text(
                "탈퇴하기",
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
                    .clickable {
                        println("탈퇴하였습니다")
                    },
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun MenuButtonCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = white,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, cardStroke)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(white),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides main) {
                    icon()
                }
            }

            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = black)
        }
    }
}
