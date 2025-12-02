package com.a307.linkcare.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text

@Composable
fun CaloriesText(calories: Double?) {
    Column {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (calories != null) {
                Text(
                    text = formatCalories(calories),
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    fontSize = 20.sp
                )
            } else {
                Text(text = "--",
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray)
            }
            Text(
                text = "cal",
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun CaloriesTextPreview() {
    CaloriesText(calories = 75.0)
}
