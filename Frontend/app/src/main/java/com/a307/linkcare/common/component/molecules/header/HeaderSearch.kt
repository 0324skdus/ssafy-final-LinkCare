package com.a307.linkcare.common.component.molecules.header

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*

/**
 * # 검색 헤더
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    placeholder: String = "모임 검색하기",
    focusOnShow: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.header_arrow_down_icon),
                    contentDescription = "back",
                    tint = black,
                    modifier = Modifier.size(25.dp)
                )
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(placeholder, color = Color(0xFFBDBDBD)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = black,
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(
                                painter = painterResource(R.drawable.header_search_clear),
                                contentDescription = "clear",
                                tint = black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )

            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = white
        )
    )

    LaunchedEffect(focusOnShow) {
        if (focusOnShow) focusRequester.requestFocus()
    }
}