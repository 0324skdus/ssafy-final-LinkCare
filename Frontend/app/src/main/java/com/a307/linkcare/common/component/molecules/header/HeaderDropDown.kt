package com.a307.linkcare.common.component.molecules.header


import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
 * # 드롭다운 타이틀 + 우측 (검색, 더보기)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderDropDown(
    title: String,
    menuItems: List<String>,
    onMenuSelect: (index: Int, label: String) -> Unit,
    onAddClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    moreMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    @DrawableRes dropIcon: Int = R.drawable.header_drop_down_icon,
    @DrawableRes searchIcon: Int = R.drawable.header_search_icon,
    @DrawableRes moreIcon: Int = R.drawable.header_menu_icon,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = title,
                        color = headerContent,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        painter = painterResource(dropIcon),
                        contentDescription = "open",
                        tint = headerContent,
                        modifier = Modifier.padding(start = 4.dp).size(25.dp)
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                        containerColor = white,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val visibleItems = menuItems.filterNot { it == title }

                        visibleItems.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    expanded = false
                                    val originalIndex = menuItems.indexOf(label)
                                    onMenuSelect(originalIndex, label)
                                }
                            )
                        }
                        if (visibleItems.isNotEmpty()) {
                            Divider()
                        }
                        DropdownMenuItem(
                            text = { Text("+ 그룹 추가하기", color = MaterialTheme.colorScheme.primary) },
                            onClick = { expanded = false; onAddClick() }
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(painterResource(searchIcon), contentDescription = "search", tint = headerContent, modifier = Modifier.size(25.dp))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(painterResource(moreIcon), contentDescription = "more", modifier = Modifier.size(25.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = white,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    moreMenu?.invoke { menuExpanded = false }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = headerContainer,
            titleContentColor = headerContent
        )
    )
}
