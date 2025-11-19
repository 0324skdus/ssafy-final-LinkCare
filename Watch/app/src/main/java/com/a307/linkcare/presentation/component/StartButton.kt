package com.a307.linkcare.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import com.a307.linkcare.R

@Composable
fun StartButton(onStartClick: () -> Unit) {
    FilledIconButton(
        onClick = onStartClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0xFF4A89F6),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(id = R.string.start_button_cd)
        )
    }
}

@Preview
@Composable
fun StartButtonPreview() {
    StartButton { }
}
