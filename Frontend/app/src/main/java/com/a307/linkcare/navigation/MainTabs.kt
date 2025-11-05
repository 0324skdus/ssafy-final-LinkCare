package com.a307.linkcare.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation

import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.unActiveField
import com.a307.linkcare.feature.commongroup.MainWithNoGroup
import com.a307.linkcare.feature.caregroup.CareGroupCreate
import com.a307.linkcare.feature.healthgroup.HealthGroupCreate
import com.a307.linkcare.feature.healthgroup.HealthGroupHome
import com.a307.linkcare.feature.healthgroup.Member
import com.a307.linkcare.R


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
    val careGroups = listOf("307", "2번째 그룹", "3번째 그룹", "4번째 그룹", "5번째 그룹")
    val currentGroupCare =
        if (careGroups.isNotEmpty() && careGroups.any { it.isNotBlank() }) careGroups[selectedIndexCare] else "케어"

    val healthGroups = listOf("만수무강", "2번째 그룹", "3번째 그룹", "4번째 그룹", "5번째 그룹")
    val currentGroupHealth =
        if (healthGroups.isNotEmpty() && healthGroups.any { it.isNotBlank() }) healthGroups[selectedIndexHealth] else "헬스"

    var selectedHealthGroup by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (selectedHealthGroup == null && healthGroups.isNotEmpty()) {
            selectedHealthGroup = healthGroups.first()
        }
    }

    Scaffold(
        topBar = {
            when (currentDestination?.route) {
                // 케어 그룹 생성 화면
                Route.CreateCareGroup.route -> {
                    HeaderBack(
                        title = "케어 그룹 생성",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // 헬스 그룹 생성 화면
                Route.CreateHealthGroup.route -> {
                    // 헤더 없음
                    Column {
                        HeaderBack(
                            title = "헬스 그룹 생성",
                            onBackClick = { navController.popBackStack() }
                        )
                        Divider(color = unActiveField, thickness = 1.dp)
                    }
                }

                else -> {
                    Column {
                        when {
                            currentDestination?.hierarchy?.any { it.route == Route.Care.route } == true -> {
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
                                            onAddClick = { navController.navigate(Route.CreateCareGroup.route) },
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
                            currentDestination?.hierarchy?.any { it.route == Route.Health.route } == true -> {
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
                                            onAddClick = { navController.navigate(Route.CreateHealthGroup.route) },
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

                            currentDestination?.route == Route.Alarm.route -> HeaderTitleOnly(title = "알림")
                            currentDestination?.route == Route.MyPage.route -> HeaderTitleOnly(title = "내 페이지")
                        }
                        // 헤더 아래 회색 구분선
                        Divider(
                            color = Color(0xFFD9D9D9),
                            thickness = 1.dp
                        )
                    }
                }
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
            // 케어 탭 그래프
            navigation(
                route = Route.Care.route,
                startDestination = "care/main"
            ) {
                composable("care/main") {
                    MainWithNoGroup(
                        navController = navController,
                        onCreateGroupClick = {
                            navController.navigate(Route.CreateCareGroup.route)
                        },
                        currentTab = "care"
                    )
                }
                composable(Route.CreateCareGroup.route) {
                    CareGroupCreate(navController = navController)
                }
            }

            // 헬스 탭 그래프
            navigation(
                route = Route.Health.route,
                startDestination = "health/main"
            ) {
                composable("health/main") {
                    if (selectedHealthGroup == null) {
                        MainWithNoGroup(
                            navController = navController,
                            onCreateGroupClick = {
                                navController.navigate(Route.CreateHealthGroup.route)
                            },
                            currentTab = "health"
                        )
                    } else {
                        HealthGroupHome(
                            membersAll = listOf(
                                Member(
                                    "김싸피(나)",
                                    10001,
                                    30000,
                                    R.drawable.char_bear_1,
                                    "컨디션 좋아요!",
                                    isLeader = true
                                ),
                                Member("김아이", 13001, 30000, R.drawable.char_bear_2, "오늘은 좀 피곤해요"),
                                Member("김에이", 15000, 30000, R.drawable.char_bear_3, "잘 걸었다!"),
                                Member("김삼성", 11000, 30000, R.drawable.char_duck_1, "저녁 산책 완료 🐾")
                            )
                        )
                    }
                }
                composable(Route.CreateHealthGroup.route) {
                    HealthGroupCreate(navController = navController)
                }
            }

            composable(Route.Alarm.route) { /* ... */ }
            composable(Route.MyPage.route) { /* ... */ }
        }
    }
}