package com.a307.linkcare.common.component.molecules.header


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.a307.linkcare.common.theme.*

private val headerContainer = white
private val headerContent   = black

/**
 * # 타이틀만
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderTitleOnly(title: String) {
    CenterAlignedTopAppBar(
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
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = headerContainer,
            titleContentColor = headerContent
        )
    )
}