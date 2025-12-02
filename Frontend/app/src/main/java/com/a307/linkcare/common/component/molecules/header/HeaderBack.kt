package com.a307.linkcare.common.component.molecules.header


import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*

private val headerContainer = white
private val headerContent   = black

/**
 * # 뒤로가기 + 타이틀
 *  ex) "케어 모임 생성"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBack(
    title: String,
    onBackClick: () -> Unit,
    @DrawableRes backIcon: Int = R.drawable.header_arrow_down_icon,
    actions: (@Composable () -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(painterResource(backIcon), contentDescription = "back", tint = headerContent, modifier = Modifier.size(25.dp))
            }
        },
        title = {
            Text(
                text = title,
                color = headerContent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight  = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = headerContainer,
            titleContentColor = headerContent
        )
    )
}