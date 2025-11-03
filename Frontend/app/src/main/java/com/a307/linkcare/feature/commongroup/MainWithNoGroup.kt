package com.a307.linkcare.feature.commongroup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*

@Composable
fun MainWithNoGroup(
    navController: NavHostController,
    @DrawableRes duckRes: Int = R.drawable.empty_duck,
    onCreateGroupClick: () -> Unit = {},
    onExploreGroupClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(duckRes),
            contentDescription = null,
            modifier = Modifier
                .height(160.dp)
                .width(160.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "아직 가입된 그룹이 없습니다.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateGroupClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(140.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                        containerColor = main,
                        contentColor = white
                )
            ) { Text("새 그룹 만들기") }

            Button(
                onClick = onExploreGroupClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(140.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8C8C8C),
                    contentColor = white
                )
            ) { Text("그룹 탐색하기") }
        }
    }
}
