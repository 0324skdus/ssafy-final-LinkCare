package com.a307.linkcare.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.a307.linkcare.R
import com.a307.linkcare.common.component.molecules.*
import com.a307.linkcare.common.theme.unActiveField
import com.a307.linkcare.feature.caregroup.CareGroup
import com.a307.linkcare.feature.caregroup.CareGroupCreate
import com.a307.linkcare.feature.caregroup.CareGroupEdit
import com.a307.linkcare.feature.caregroup.CareGroupHome
import com.a307.linkcare.feature.caregroup.GroupMember
import com.a307.linkcare.feature.caregroup.domain.ShareOptions
import com.a307.linkcare.feature.commongroup.GroupSearch
import com.a307.linkcare.feature.commongroup.MainWithNoGroup
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.healthgroup.HealthGroup
import com.a307.linkcare.feature.healthgroup.HealthGroupCreate
import com.a307.linkcare.feature.healthgroup.HealthGroupEdit
import com.a307.linkcare.feature.healthgroup.HealthGroupHome
import com.a307.linkcare.feature.notification.Notification

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    // 그룹 더미 데이터
    val careGroups = listOf("307", "2번째 그룹", "3번째 그룹")
    val healthGroups = listOf("만수무강", "2번째 그룹", "3번째 그룹")

    var selectedIndexCare by remember { mutableStateOf(0) }
    var selectedIndexHealth by remember { mutableStateOf(0) }

    val currentGroupCare =
        if (careGroups.isNotEmpty()) careGroups[selectedIndexCare] else "케어"
    val currentGroupHealth =
        if (healthGroups.isNotEmpty()) healthGroups[selectedIndexHealth] else "헬스"

    // 현재 선택 그룹 상태
    var selectedHealthGroup by remember { mutableStateOf<String?>(null) }
    var selectedCareGroup by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (selectedHealthGroup == null && healthGroups.isNotEmpty())
            selectedHealthGroup = healthGroups.first()
        if (selectedCareGroup == null && careGroups.isNotEmpty())
            selectedCareGroup = careGroups.first()
    }

    // 검색 상태
    val routeStr = currentDestination?.route.orEmpty()
    val isCareSearch = routeStr == Route.CareSearch
    val isHealthSearch = routeStr == Route.HealthSearch
    var careSearchQuery by remember { mutableStateOf("") }
    var healthSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            when {
                // --- 케어 그룹 검색 화면 ---
                isCareSearch -> {
                    HeaderSearch(
                        query = careSearchQuery,
                        onQueryChange = { careSearchQuery = it },
                        onBackClick = { navController.popBackStack() },
                        onClearClick = { careSearchQuery = "" },
                        placeholder = "케어 그룹 검색",
                        focusOnShow = true
                    )
                }

                // --- 헬스 그룹 검색 화면 ---
                isHealthSearch -> {
                    HeaderSearch(
                        query = healthSearchQuery,
                        onQueryChange = { healthSearchQuery = it },
                        onBackClick = { navController.popBackStack() },
                        onClearClick = { healthSearchQuery = "" },
                        placeholder = "헬스 그룹 검색",
                        focusOnShow = true
                    )
                }

                // --- 케어 그룹 생성 ---
                currentDestination?.route == Route.CreateCareGroup.route -> {
                    HeaderBack(
                        title = "케어 그룹 생성",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // --- 헬스 그룹 생성 ---
                currentDestination?.route == Route.CreateHealthGroup.route -> {
                    HeaderBack(
                        title = "헬스 그룹 생성",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // --- 케어 그룹 수정 ---
                currentDestination?.route?.startsWith(Route.EditCareGroup.route) == true -> {
                    HeaderBack(
                        title = "그룹 수정",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // --- 헬스 그룹 수정 ---
                currentDestination?.route?.startsWith(Route.EditHealthGroup.route) == true -> {
                    HeaderBack(
                        title = "그룹 수정",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                else -> {
                    Column {
                        when {
                            // 케어 탭
                            currentDestination?.hierarchy?.any { it.route == Route.Care.route } == true -> {
                                HeaderDropDown(
                                    title = currentGroupCare,
                                    menuItems = careGroups,
                                    onMenuSelect = { idx, _ -> selectedIndexCare = idx },
                                    onAddClick = { navController.navigate(Route.CreateCareGroup.route) },
                                    onSearchClick = { navController.navigate(Route.CareSearch) },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = {
                                                dismiss()
                                                val currentId = careGroups[selectedIndexCare]
                                                navController.navigate(
                                                    Route.EditCareGroup.withArgs(currentId)
                                                )
                                            }
                                        )
                                    }
                                )
                            }

                            // 헬스 탭
                            currentDestination?.hierarchy?.any { it.route == Route.Health.route } == true -> {
                                HeaderDropDown(
                                    title = currentGroupHealth,
                                    menuItems = healthGroups,
                                    onMenuSelect = { idx, _ -> selectedIndexHealth = idx },
                                    onAddClick = { navController.navigate(Route.CreateHealthGroup.route) },
                                    onSearchClick = { navController.navigate(Route.HealthSearch) },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = {
                                                dismiss()
                                                val currentId = healthGroups[selectedIndexHealth]
                                                navController.navigate(
                                                    Route.EditHealthGroup.withArgs(currentId)
                                                )
                                            }
                                        )
                                    }
                                )
                            }

                            // 기타 탭
                            currentDestination?.route == Route.Alarm.route ->
                                HeaderTitleOnly(title = "알림")

                            currentDestination?.route == Route.MyPage.route ->
                                HeaderTitleOnly(title = "내 페이지")
                        }
                        Divider(color = Color(0xFFD9D9D9), thickness = 1.dp)
                    }
                }
            }
        },

        bottomBar = {
            BottomBar(navController = navController, currentDestination = currentDestination)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Care.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ---------- 케어 탭 ----------
            navigation(route = Route.Care.route, startDestination = "care/main") {
                composable("care/main") {
                    if (selectedCareGroup == null) {
                        MainWithNoGroup(
                            navController = navController,
                            onCreateGroupClick = { navController.navigate(Route.CreateCareGroup.route) },
                            currentTab = "care"
                        )
                    } else {
                        CareGroupHome(
                            membersAll = listOf(
                                Member("김싸피(나)", 10001, 30000, R.drawable.char_bear_1, "컨디션 좋아요!", isLeader = true),
                                Member("김아이", 13001, 30000, R.drawable.char_bear_2, "오늘은 좀 피곤해요"),
                                Member("김에이", 15000, 30000, R.drawable.char_bear_3, "잘 걸었다!"),
                                Member("김삼성", 11000, 30000, R.drawable.char_duck_1, "저녁 산책 완료 🐾")
                            )
                        )
                    }
                }

                composable(Route.CareSearch) {
                    GroupSearch(
                        kind = "care",
                        query = careSearchQuery,
                        onQueryChange = { careSearchQuery = it },
                        onBack = { navController.popBackStack() },
                        onJoinClick = { /* TODO */ }
                    )
                }

                composable(Route.CreateCareGroup.route) {
                    CareGroupCreate(navController = navController)
                }

                composable(
                    route = Route.EditCareGroup.route,
                    arguments = Route.EditCareGroup.arguments
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString(Route.EditCareGroup.ARG_GROUP_ID)
                    CareGroupEdit(
                        navController = navController,
                        initial = mockCareGroup(groupId ?: "307"),
                        isLeader = false
                    )
                }
            }

            // ---------- 헬스 탭 ----------
            navigation(route = Route.Health.route, startDestination = "health/main") {
                composable("health/main") {
                    if (selectedHealthGroup == null) {
                        MainWithNoGroup(
                            navController = navController,
                            onCreateGroupClick = { navController.navigate(Route.CreateHealthGroup.route) },
                            currentTab = "health"
                        )
                    } else {
                        HealthGroupHome(
                            membersAll = listOf(
                                Member("김싸피(나)", 10001, 30000, R.drawable.char_bear_1, "컨디션 좋아요!", isLeader = true),
                                Member("김아이", 13001, 30000, R.drawable.char_bear_2, "오늘은 좀 피곤해요"),
                                Member("김에이", 15000, 30000, R.drawable.char_bear_3, "잘 걸었다!"),
                                Member("김삼성", 11000, 30000, R.drawable.char_duck_1, "저녁 산책 완료 🐾")
                            )
                        )
                    }
                }

                composable(Route.HealthSearch) {
                    GroupSearch(
                        kind = "health",
                        query = healthSearchQuery,
                        onQueryChange = { healthSearchQuery = it },
                        onBack = { navController.popBackStack() },
                        onJoinClick = { /* TODO */ }
                    )
                }

                composable(Route.CreateHealthGroup.route) {
                    HealthGroupCreate(navController = navController)
                }

                composable(
                    route = Route.EditHealthGroup.route,
                    arguments = Route.EditHealthGroup.arguments
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString(Route.EditHealthGroup.ARG_GROUP_ID)
                    HealthGroupEdit(
                        navController = navController,
                        initial = mockHealthGroup(groupId ?: "만수무강"),
                        isLeader = true
                    )
                }
            }

            composable(Route.Alarm.route) {
                Notification()
            }
            composable(Route.MyPage.route) { /* ... */ }
        }
    }
}

/** 더미 데이터 */
private fun mockCareGroup(id: String) = CareGroup(
    id = id.toLongOrNull() ?: 0L,
    name = "그룹$id",
    description = "샘플 설명입니다.",
    maxMember = 6,
    inviteLink = "https://linkcare.app/invite/$id",
    members = listOf(
        GroupMember(1, "김싸피(나)", R.drawable.char_bear_1, isLeader = true),
        GroupMember(2, "김아이", R.drawable.char_bear_2, isLeader = false)
    ),
    share = ShareOptions(
        exercise = true,
        heartRate = true,
        steps = true,
        sleep = true,
        water = true,
        bloodPressure = true,
        bloodSugar = true
    )
)

private fun mockHealthGroup(id: String) = HealthGroup(
    id = id.toLongOrNull() ?: 0L,
    name = "그룹$id",
    description = "샘플 설명입니다.",
    maxMember = 6,
    inviteLink = "https://linkcare.app/invite/$id",
    members = listOf(
        GroupMember(1, "김싸피(나)", R.drawable.char_bear_1, isLeader = true),
        GroupMember(2, "김아이", R.drawable.char_bear_2, isLeader = false)
    ),
    share = com.a307.linkcare.feature.healthgroup.domain.ShareOptions(
        exercise = true,
        heartRate = true,
        steps = true
    )
)
