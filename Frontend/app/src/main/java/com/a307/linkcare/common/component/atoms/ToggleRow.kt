package com.a307.linkcare.common.component.atoms

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.a307.linkcare.common.theme.*


@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean,
    isRequired: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) black else Color(0xFF8E8E93)
            )
            if (isRequired) {
                Text(
                    text = "필수",
                    style = MaterialTheme.typography.bodySmall,
                    color = main,
                    modifier = Modifier
                        .background(
                            color = main.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.zIndex(1f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = white,
                checkedTrackColor = main.copy(alpha = 0.9f),
                uncheckedThumbColor = Color(0xFFE5E5EA),
                uncheckedTrackColor = Color(0xFFCCCCCC)
            )
        )
    }
}
