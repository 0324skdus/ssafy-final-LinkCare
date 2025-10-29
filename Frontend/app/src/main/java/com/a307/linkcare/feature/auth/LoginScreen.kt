package com.a307.linkcare.feature.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.main

@Composable
fun LinkCareLoginScreen(
    onLoginSuccess: () -> Unit = {},
    onSignupClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(main)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // logo
            Text(
                text = "LinkCare",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            val fieldShape = RoundedCornerShape(40.dp)
            val fieldBorder = Color(0x29000000)

            // email input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(fieldShape)
                    .background(Color.White)
                    .border(1.dp, fieldBorder, fieldShape)
            ) {
                Text(
                    text = "이메일",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x29000000),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // pw input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(fieldShape)
                    .background(Color.White)
                    .border(1.dp, fieldBorder, fieldShape)
            ) {
                Text(
                    text = "비밀번호",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x29000000),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // login button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(fieldShape)
                    .background(Color(0xFFEAEAEA))
                    .clickable { onLoginSuccess() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "로그인",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC3C3C3)
                )
            }

            Spacer(Modifier.height(36.dp))

            // line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(
                    color = Color.White,
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "또는",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Divider(
                    color = Color.White,
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // SNS login buttons
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

            // signup description
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "계정이 없으신가요?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "회원가입",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onSignupClick() }
                )
            }
        }

        // main duck image
        Image(
            painter = painterResource(id = R.drawable.main_duck),
            contentDescription = "main_duck",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 420.dp, height = 288.dp)
                .offset(y = 20.dp),
            contentScale = ContentScale.Fit
        )
    }
}
