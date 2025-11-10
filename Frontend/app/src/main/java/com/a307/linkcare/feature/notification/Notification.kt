package com.a307.linkcare.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Notification() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("콕 찌르기", "편지함", "그룹")

    Column(Modifier.fillMaxSize()) {
        // 상단 탭
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF4A89F6)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // 탭별 화면
        when (selectedTab) {
            0 -> NotiList()
            1 -> LetterList()
            2 -> GroupList()
        }
    }
}
