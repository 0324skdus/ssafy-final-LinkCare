package com.a307.linkcare.common.component.molecules.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.theme.*

@Composable
fun PagerDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = black,
    inactiveColor: Color = Color.LightGray,
    clickable: Boolean = true,
    onDotClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (selected) 20.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) activeColor else inactiveColor)
                    .then(
                        if (clickable && onDotClick != null)
                            Modifier.clickable { onDotClick(index) }
                        else Modifier
                    )
            )
            if (index != total - 1) Spacer(Modifier.width(8.dp))
        }
    }
}
