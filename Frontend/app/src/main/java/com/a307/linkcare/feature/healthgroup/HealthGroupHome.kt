package com.a307.linkcare.feature.healthgroup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** TO DO */
@Composable
fun HealthGroupHome(
    groupName: String,
    onShowSearch: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = groupName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        // ex) 상단 목표 카드
        ElevatedCard(Modifier.fillMaxWidth()) { /* ... */ }
        Spacer(Modifier.height(12.dp))
        // ex) 탭(‘회원현황(오늘)’ / ‘이번달 일별’) + 리스트
        // ex) 미션/알림 카드 등…
    }
}
