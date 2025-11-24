package com.a307.linkcare.feature.notification.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.feature.notification.ui.list.GroupList
import com.a307.linkcare.feature.notification.ui.list.LetterList
import com.a307.linkcare.feature.notification.ui.list.NotiList
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@Composable
fun Notification(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("콕 찌르기", "편지함", "그룹")
    val refreshing by viewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()


    fun refresh() {
        coroutineScope.launch {
            when (selectedTab) {
                0, 1 -> viewModel.loadAllAlarms()
                2 -> viewModel.loadNotifications("GROUP")
            }
        }
    }

    // 화면이 처음 나타날 때와 탭이 변경될 때 데이터 로드
    LaunchedEffect(selectedTab) {
        refresh()
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing),
        onRefresh = { refresh() }) {
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
                0 -> NotiList(viewModel = viewModel)
                1 -> LetterList(viewModel = viewModel)
                2 -> GroupList()
            }
        }
    }
}
