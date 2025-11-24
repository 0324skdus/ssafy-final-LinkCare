package com.a307.linkcare.common.component.molecules.header

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.white

/**
 * # 기본 헤더: 가운데 타이틀 + 우측 (검색, 더보기)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBasic(
    title: String,
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    moreMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    @DrawableRes searchIcon: Int = R.drawable.header_search_icon,
    @DrawableRes moreIcon: Int = R.drawable.header_menu_icon,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(painterResource(searchIcon), contentDescription = "search", modifier = Modifier.size(25.dp))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(painterResource(moreIcon), contentDescription = "more", modifier = Modifier.size(25.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    containerColor = white,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    moreMenu?.invoke { menuExpanded = false }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = white
        )
    )
}