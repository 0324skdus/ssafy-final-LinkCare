package com.a307.linkcare.common.component.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.theme.black
import com.a307.linkcare.common.theme.white


@Composable
fun SummaryChipList(items: List<String>) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        items.forEach { text ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = white,
                tonalElevation = 2.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text, fontWeight = FontWeight.Medium, color = black)
                }
            }
        }
    }
}