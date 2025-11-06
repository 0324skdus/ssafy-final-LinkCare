package com.a307.linkcare.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white

@Composable
fun NotiList() {
    val nudges = listOf(
        "정혜예: 파이팅!",
        "김혜윤: 오늘도 힘내!",
        "진혜예: 지금 뭐하고 있어?"
    )

    Column(Modifier.fillMaxSize()) {
        // 리스트
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(nudges) { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tap),
                        contentDescription = "콕 찌르기 아이콘",
                        modifier = Modifier.size(25.dp),
                        tint = Color.Unspecified
                    )

                    Spacer(Modifier.width(12.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = black
                    )
                }
                Divider(color = Color.LightGray.copy(alpha = 0.4f))
            }
        }

        // 하단 버튼
        Button(
            onClick = { /* 모두 읽음 처리 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = main,
                contentColor = white
            )
        ) {
            Text("모두 읽음")
        }
    }
}
