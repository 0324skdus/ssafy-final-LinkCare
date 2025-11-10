package com.a307.linkcare.common.component.page

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*

@Composable
fun ErrorScreen(
    navController: NavHostController,
    currentTab: String,
    @DrawableRes duckRes: Int = R.drawable.empty_duck
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
            text = "에러가 발생했습니다.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            color = black
        )
    }
}
