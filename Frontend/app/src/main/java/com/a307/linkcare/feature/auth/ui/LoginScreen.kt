package com.a307.linkcare.feature.auth.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: (isFirstLogin: Boolean) -> Unit = {},
    onSignupClick: () -> Unit = {}
) {
    val isFirstLogin = false // TO DO: 첫 로그인

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val duckAspect = 462f / 281f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(main)
    ) {
        val duckHeight = maxWidth / duckAspect
        val contentHeight = (maxHeight - duckHeight).coerceAtLeast(0.dp)

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
                Text(
                    text = "LinkCare",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = white,
                    modifier = Modifier.padding(top = 150.dp, bottom = 32.dp)
                )

                LoginForm(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it }
                )

                Spacer(Modifier.height(20.dp))

                LcBtn(
                    text = "로그인",
                    buttonColor = point,
                    onClick = { onLoginSuccess(isFirstLogin) },
                    isEnabled = email.isNotBlank() && password.isNotBlank()
                )

                Spacer(Modifier.height(36.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(color = white, thickness = 1.dp, modifier = Modifier.weight(1f))
                    Text(
                        text = "또는",
                        color = white,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Divider(color = white, thickness = 1.dp, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_login),
                        contentDescription = "google",
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFEE500)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.kakao_login),
                            contentDescription = "kakao",
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "계정이 없으신가요?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = white
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "회원가입",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onSignupClick() }
                    )
                }
            }
        }

        // 오리 이미지
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


@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    LcInputField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = "이메일"
    )
    Spacer(modifier = Modifier.height(16.dp))
    LcInputField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "비밀번호",
        isPassword = true
    )
}
