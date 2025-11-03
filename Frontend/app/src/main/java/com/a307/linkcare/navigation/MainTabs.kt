package com.a307.linkcare.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.unActiveField
import com.a307.linkcare.feature.commongroup.MainWithNoGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(
    onAddGroup: () -> Unit = {},
    onEditGroup: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    // 케어 탭 상태
    var isSearchingCare by remember { mutableStateOf(false) }
    var queryCare by remember { mutableStateOf("") }
    var selectedIndexCare by remember { mutableStateOf(0) }

    // 헬스 탭 상태
    var isSearchingHealth by remember { mutableStateOf(false) }
    var queryHealth by remember { mutableStateOf("") }
    var selectedIndexHealth by remember { mutableStateOf(0) }

    // 케어/헬스 그룹 리스트 - 더미 데이터
    /** TO DO : 그룹 조회 API 연결 */
    val careGroups = listOf("") //"307", "2번째 그룹", "3번째 그룹", "4번째 그룹", "5번째 그룹"
    val currentGroupCare = if (careGroups.isNotEmpty() && careGroups.any { it.isNotBlank() }) careGroups[selectedIndexCare] else "케어"

    val healthGroups = listOf("만수무강", "2번째 그룹", "3번째 그룹", "4번째 그룹", "5번째 그룹")
    val currentGroupHealth = if (healthGroups.isNotEmpty() && healthGroups.any { it.isNotBlank() }) healthGroups[selectedIndexHealth] else "헬스"

    Scaffold(
        topBar = {
            Column {
                when (currentDestination?.route) {

                    // 케어 탭
                    Route.Care.route -> {
                        if (isSearchingCare) {
                            HeaderSearch(
                                query = queryCare,
                                onQueryChange = { queryCare = it },
                                onBackClick = { isSearchingCare = false },
                                onClearClick = { queryCare = "" },
                                placeholder = "그룹 검색하기",
                                focusOnShow = true
                            )
                        } else {
                            if (careGroups.isNotEmpty() && careGroups.any { it.isNotBlank() }) {
                                HeaderDropDown(
                                    title = currentGroupCare,
                                    menuItems = careGroups,
                                    onMenuSelect = { idx, _ -> selectedIndexCare = idx },
                                    onAddClick = onAddGroup,
                                    onSearchClick = { isSearchingCare = true },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = { dismiss(); onEditGroup() }
                                        )
                                    }
                                )
                            } else {
                                HeaderBasic(
                                    title = "케어",
                                    onSearchClick = { isSearchingCare = true },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = { dismiss(); onEditGroup() }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 헬스 탭
                    Route.Health.route -> {
                        if (isSearchingHealth) {
                            HeaderSearch(
                                query = queryHealth,
                                onQueryChange = { queryHealth = it },
                                onBackClick = { isSearchingHealth = false },
                                onClearClick = { queryHealth = "" },
                                placeholder = "그룹 검색하기",
                                focusOnShow = true
                            )
                        } else {
                            if (healthGroups.isNotEmpty() && healthGroups.any { it.isNotBlank() }) {
                                HeaderDropDown(
                                    title = currentGroupHealth,
                                    menuItems = healthGroups,
                                    onMenuSelect = { idx, _ -> selectedIndexHealth = idx },
                                    onAddClick = onAddGroup,
                                    onSearchClick = { isSearchingHealth = true },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = { dismiss(); onEditGroup() }
                                        )
                                    }
                                )
                            } else {
                                HeaderBasic(
                                    title = "헬스",
                                    onSearchClick = { isSearchingHealth = true },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = { dismiss(); onEditGroup() }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Route.Alarm.route -> HeaderTitleOnly(title = "알림")
                    Route.MyPage.route -> HeaderTitleOnly(title = "내 페이지")
                    else -> {}
                }

                // 헤더 아래 회색 구분선
                Divider(
                    color = Color(0xFFD9D9D9),
                    thickness = 1.dp
                )
            }
        },

        bottomBar = {
            BottomBar(
                navController = navController,
                currentDestination = currentDestination
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Care.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Care.route) {
                MainWithNoGroup(
                    navController = navController,
                    onCreateGroupClick = { /* TODO */ },
                    onExploreGroupClick = { isSearchingCare = true }
                )
            }

            composable(Route.Health.route) {
                MainWithNoGroup(
                    navController = navController,
                    onCreateGroupClick = { /* TODO */ },
                    onExploreGroupClick = { isSearchingHealth = true }
                )
            }

            composable(Route.Alarm.route)  { /* TODO */ }
            composable(Route.MyPage.route) { /* TODO */ }
        }
    }
}
