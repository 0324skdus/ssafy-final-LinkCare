package com.a307.linkcare.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.a307.linkcare.common.component.molecules.BottomBar
import com.a307.linkcare.common.component.molecules.HeaderBack
import com.a307.linkcare.common.component.molecules.HeaderDropDown
import com.a307.linkcare.common.component.molecules.HeaderSearch
import com.a307.linkcare.common.component.molecules.HeaderTitleOnly
import com.a307.linkcare.common.theme.unActiveField
import com.a307.linkcare.feature.caregroup.CareGroup
import com.a307.linkcare.feature.caregroup.CareGroupCreate
import com.a307.linkcare.feature.caregroup.CareGroupEdit
import com.a307.linkcare.feature.caregroup.CareGroupHome
import com.a307.linkcare.feature.caregroup.GroupMember
import com.a307.linkcare.feature.caregroup.domain.ShareOptions
import com.a307.linkcare.feature.commongroup.ui.GroupSearch
import com.a307.linkcare.feature.commongroup.ui.MainWithNoGroup
import com.a307.linkcare.feature.commongroup.domain.Member
import com.a307.linkcare.feature.healthgroup.HealthGroup
import com.a307.linkcare.feature.healthgroup.HealthGroupCreate
import com.a307.linkcare.feature.healthgroup.HealthGroupEdit
import com.a307.linkcare.feature.healthgroup.HealthGroupHome
import com.a307.linkcare.feature.mypage.DecorateScreen
import com.a307.linkcare.feature.mypage.MyGroups
import com.a307.linkcare.feature.mypage.MyPage
import com.a307.linkcare.feature.mypage.MyPageUiState
import com.a307.linkcare.feature.mypage.StoreItem
import com.a307.linkcare.feature.mypage.StoreScreen
import com.a307.linkcare.feature.mypage.StoreUiState
import com.a307.linkcare.feature.notification.Notification
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.mypage.Gender
import com.a307.linkcare.feature.mypage.Profile
import com.a307.linkcare.feature.mypage.ProfileEditScreen

private fun NavController.goBackToMyPage() {
    navigate(Route.MyPage.route) {
        popUpTo(Route.MyPage.route) { inclusive = false }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(
    rootNavController: NavHostController,
    groupvm: MyGroupsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    // CARE / HEALTH 데이터
    val careGroups by groupvm.careGroups.collectAsState()
    val healthGroups by groupvm.healthGroups.collectAsState()

    var selectedIndexCare by remember { mutableStateOf(0) }
    var selectedIndexHealth by remember { mutableStateOf(0) }

    // 현재 선택된 그룹 이름 표시
    val currentGroupCare =
        careGroups.getOrNull(selectedIndexCare)?.groupName ?: "케어"

    val currentGroupHealth =
        healthGroups.getOrNull(selectedIndexHealth)?.groupName ?: "헬스"

    var selectedCareGroup by remember { mutableStateOf<Long?>(null) }
    var selectedHealthGroup by remember { mutableStateOf<Long?>(null) }

    // 첫 진입 시 API 호출
    LaunchedEffect(Unit) {
        groupvm.loadCare()
        groupvm.loadHealth()
    }

    LaunchedEffect(careGroups) {
        if (selectedCareGroup == null && careGroups.isNotEmpty()) {
            selectedCareGroup = careGroups.first().groupSeq
        }
    }

    LaunchedEffect(healthGroups) {
        if (selectedHealthGroup == null && healthGroups.isNotEmpty()) {
            selectedHealthGroup = healthGroups.first().groupSeq
        }
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

                // --- 마이페이지 ---
                currentDestination?.route == Route.MyPageMain.route -> {
                    Column {
                        HeaderTitleOnly(title = "내 페이지")
                        Divider(color = unActiveField, thickness = 1.dp)
                    }
                }

                currentDestination?.route == Route.EditProfile.route -> {
                    Column {
                        HeaderBack(
                            title = "개인정보 편집",
                            onBackClick = { navController.popBackStack() }
                        )

                        Divider(color = unActiveField, thickness = 1.dp)
                    }
                }

                // --- 그룹 조회 (마이페이지 > 그룹 조회) ---
                currentDestination?.route == Route.MyGroups.route -> {
                    HeaderBack(
                        title = "그룹 조회",
                        onBackClick = { navController.popBackStack() }
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // --- 상점 (마이페이지 > 상점) ---
                currentDestination?.route == Route.Store.route -> {
                    HeaderBack(
                        title = "상점",
                        onBackClick = { navController.goBackToMyPage() } // 여기!
                    )
                    Divider(color = unActiveField, thickness = 1.dp)
                }

                // --- 꾸미기 (마이페이지 > 꾸미기) ---
                currentDestination?.route == Route.Decorate.route -> {
                    HeaderBack(
                        title = "꾸미기",
                        onBackClick = { navController.goBackToMyPage() }
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
                                    menuItems = careGroups.map { it.groupName },
                                    onMenuSelect = { idx, _ -> selectedIndexCare = idx },
                                    onAddClick = { navController.navigate(Route.CreateCareGroup.route) },
                                    onSearchClick = { navController.navigate(Route.CareSearch) },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = {
                                                dismiss()
                                                val currentId = careGroups[selectedIndexCare].groupSeq
                                                navController.navigate(
                                                    Route.EditCareGroup.withArgs(currentId.toString())
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
                                    menuItems = healthGroups.map { it.groupName },
                                    onMenuSelect = { idx, _ -> selectedIndexHealth = idx },
                                    onAddClick = { navController.navigate(Route.CreateHealthGroup.route) },
                                    onSearchClick = { navController.navigate(Route.HealthSearch) },
                                    moreMenu = { dismiss ->
                                        DropdownMenuItem(
                                            text = { Text("그룹 수정하기") },
                                            onClick = {
                                                dismiss()
                                                val currentId = healthGroups[selectedIndexHealth].groupSeq
                                                navController.navigate(
                                                    Route.EditHealthGroup.withArgs(currentId.toString())
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
                        selectedCareGroup?.let { groupSeq ->
                            CareGroupHome(
                                groupSeq = groupSeq
                            )
                        }
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
                            ),
                            isLeader = true,            // 현재 유저가 그룹장일 때만 true
                            currentGoal = 20000,        // null이면 미설정
                            onSaveGoal = { newGoal ->
                                // TODO: API 요청으로 저장
                                // ex) viewModel.saveGroupGoal(groupId, newGoal)
                            }
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

            // ---------- 알림 ----------
            composable(Route.Alarm.route) {
                Notification()
            }

            // ---------- 마이페이지 ----------
            navigation(route = Route.MyPage.route, startDestination = Route.MyPageMain.route) {
                // MyPage 홈
                composable(Route.MyPageMain.route) {
                    MyPage(
                        state = MyPageUiState(
                            nickname = "김싸피",
                            coinLabel = "보유 코인 300",
                            groupCountLabel = "그룹 조회",
                            avatarRes = R.drawable.char_bear_1,
                            avatarBgRes = R.drawable.background_1
                        ),
                        onGroupsClick = { navController.navigate(Route.MyGroups.route) },
                        onEditClick = { navController.navigate(Route.EditProfile.route) },
                        onStoreClick = { navController.navigate(Route.Store.route) },
                        onDecorateClick = { navController.navigate(Route.Decorate.route) },
                        onLogout = {
                            rootNavController.navigate(Route.Login.route) {
                                popUpTo(rootNavController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 편집
                composable(Route.EditProfile.route) {
                    ProfileEditScreen(
                        initial = Profile(
                            name = "싸피",
                            gender = Gender.FEMALE,
                            heightCm = 162,
                            weightKg = 53,
                            birthDate = "2022-11-11"
                        ),
                        onSave = { /** updated -> viewModel.saveProfile(updated)*/ },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 그룹 조회
                composable(Route.MyGroups.route) {
                    MyGroups(navController = navController)
                }

                // 상점
                val dummyState = StoreUiState(
                    coins = 300,
                    characterItems = listOf(
                        StoreItem("bear1", R.drawable.char_bear_1, price = 100, owned = true),
                        StoreItem("duck1", R.drawable.char_duck_1, price = 150, owned = false)
                    ),
                    backgroundItems = listOf(
                        StoreItem("bg1", R.drawable.background_1, price = 80, owned = true),
                        StoreItem("bg2", R.drawable.background_2, price = 120, owned = false),
                        StoreItem("bg3", R.drawable.background_3, price = 120, owned = false),
                        StoreItem("bg4", R.drawable.background_4, price = 120, owned = false),
                        StoreItem("bg5", R.drawable.background_5, price = 120, owned = false),
                        StoreItem("bg6", R.drawable.background_6, price = 120, owned = false),
                        StoreItem("bg7", R.drawable.background_7, price = 120, owned = false),
                        StoreItem("bg8", R.drawable.background_8, price = 120, owned = false)
                    ),
                    equippedBackground = R.drawable.background_1,
                    equippedCharacter = R.drawable.char_bear_1
                )
                composable(Route.Store.route) {
                    StoreScreen(
                        state = dummyState,
                        onBuy = { /* TODO: 구매 처리 */ }
                    )
                }

                // 꾸미기
                composable(Route.Decorate.route) {
                    DecorateScreen(
                        ownedCharacters = listOf(R.drawable.char_bear_1, R.drawable.char_duck_1),
                        ownedBackgrounds = listOf(R.drawable.background_1, R.drawable.background_2),
                        equippedCharacter = R.drawable.char_bear_1,
                        equippedBackground = R.drawable.background_1,
                        onApply = { _, _ -> /* TODO: 장착 처리 */ }
                    )
                }
            }
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
