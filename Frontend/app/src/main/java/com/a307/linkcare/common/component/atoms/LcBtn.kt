package com.a307.linkcare.common.component.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.theme.*

@Composable
fun LcBtn (
    text: String,
    modifier: Modifier = Modifier,
    buttonColor: Color = main,
    buttonTextColor: Color = white,
    onClick: () -> Unit = {},
    isEnabled: Boolean = true
) {
    val shape = RoundedCornerShape(40.dp)

    val backgroundColor =
        if (!isEnabled) unActiveBtn
        else buttonColor

    val textColor =
        if (!isEnabled) unActiveField
        else buttonTextColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (isEnabled) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
