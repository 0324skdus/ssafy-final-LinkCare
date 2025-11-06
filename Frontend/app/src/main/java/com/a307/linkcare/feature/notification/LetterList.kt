package com.a307.linkcare.feature.notification

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white

data class Letter(val title: String, val sender: String, val content: String)

@Composable
fun LetterList() {
    val letters = listOf(
        Letter("라연언전가", "장혜예", "너가 요즘 블루베리도 먹고..."),
        Letter("곡연안전가", "장혜예", "너가 요즘 블루베리도 먹고...")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(letters) { letter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = white)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "편지 아이콘",
                            modifier = Modifier.size(40.dp),
                            tint = main
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${letter.title} - ${letter.sender}",
                                fontWeight = FontWeight.Bold,
                                color = black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(letter.content, maxLines = 2, color = black)
                        }
                    }
                }
            }
        }

        // 하단 버튼
        Button(
            onClick = { /* 모두 읽음 처리 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
