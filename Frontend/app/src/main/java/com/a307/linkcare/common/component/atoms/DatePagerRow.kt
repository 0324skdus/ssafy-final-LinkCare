package com.a307.linkcare.common.component.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white


@Composable
fun DatePagerRow(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = main,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // 중앙 텍스트
            Text(
                text = label,
                color = white,
                fontWeight = FontWeight.SemiBold
            )

        }
    }
}