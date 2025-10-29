package com.a307.linkcare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWithBottomBar() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("LinkCare 메인") }) }
        // bottomBar = { /* 나중에 BottomBar 추가 */ }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("여기가 로그인 후 메인 화면")
        }
    }
}
