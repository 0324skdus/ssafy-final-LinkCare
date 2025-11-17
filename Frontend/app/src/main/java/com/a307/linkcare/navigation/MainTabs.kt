package com.a307.linkcare.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
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
import com.a307.linkcare.feature.mypage.MyPageApiState
import com.a307.linkcare.feature.mypage.MyPageViewModel
import com.a307.linkcare.feature.mypage.StoreItem
import com.a307.linkcare.feature.mypage.StoreScreen
import com.a307.linkcare.feature.mypage.StoreUiState
import com.a307.linkcare.feature.notification.Notification
import com.a307.linkcare.feature.notification.data.MyFirebaseMessagingService
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a307.linkcare.common.component.molecules.HeaderBasic
import com.a307.linkcare.feature.customize.CustomEntity
import com.a307.linkcare.feature.customize.CustomizeRepoHolderVM
import com.a307.linkcare.feature.customize.CustomizeRepository
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel
import com.a307.linkcare.feature.mypage.Gender
import com.a307.linkcare.feature.mypage.Profile
import com.a307.linkcare.feature.mypage.ProfileEditScreen
import com.a307.linkcare.feature.watch.data.DataLayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CARE / HEALTH 데이터
    val careGroups by groupvm.careGroups.collectAsState()
    val healthGroups by groupvm.healthGroups.collectAsState()

    var selectedIndexCare by remember { mutableStateOf(0) }
    var selectedIndexHealth by remember { mutableStateOf(0) }

    // 현재 선택된 그룹 이름 표시 (memoized)
    val currentGroupCare = remember(careGroups, selectedIndexCare) {
        careGroups.getOrNull(selectedIndexCare)?.groupName ?: "케어"
    }

    val currentGroupHealth = remember(healthGroups, selectedIndexHealth) {
        healthGroups.getOrNull(selectedIndexHealth)?.groupName ?: "헬스"
    }

    var selectedCareGroup by remember { mutableStateOf<Long?>(null) }
    var selectedHealthGroup by remember { mutableStateOf<Long?>(null) }

    // 첫 진입 시 API 호출
    LaunchedEffect(Unit) {
        groupvm.loadCare()
        groupvm.loadHealth()
    }

    // FCM 승인 알림 수신 시 자동 새로고침
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val groupType = intent?.getStringExtra(MyFirebaseMessagingService.EXTRA_GROUP_TYPE)
                Log.d("MainTabs", "그룹 가입 승인 브로드캐스트 수신: $groupType")

                // 그룹 타입에 따라 해당 그룹 목록 새로고침
                when (groupType?.uppercase()) {
                    "CARE" -> {
                        scope.launch {
                            groupvm.loadCare()
                            Toast.makeText(context, "케어 그룹 가입이 승인되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "HEALTH" -> {
                        scope.launch {
                            groupvm.loadHealth()
                            Toast.makeText(context, "헬스 그룹 가입이 승인되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        // 타입 모를 경우 둘 다 새로고침
                        scope.launch {
                            groupvm.loadCare()
                            groupvm.loadHealth()
                            Toast.makeText(context, "그룹 가입이 승인되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(MyFirebaseMessagingService.ACTION_GROUP_JOINED)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
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
                                    onMenuSelect = { idx, _ -> selectedIndexCare = idx; selectedCareGroup = careGroups[idx].groupSeq; },
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
                                        DropdownMenuItem(
                                            text = { Text("초대 링크 복사") },
                                            onClick = {
                                                dismiss()
                                                val groupSeq = careGroups[selectedIndexCare].groupSeq
                                                scope.launch {
                                                    try {
                                                        withTimeout(10000L) {
                                                            val result = groupvm.createInvitationLink(groupSeq)
                                                            result.onSuccess { inviteLink ->
                                                                clipboardManager.setText(AnnotatedString(inviteLink))
                                                                Toast.makeText(context, "초대 링크가 복사되었습니다", Toast.LENGTH_SHORT).show()
                                                            }.onFailure { error ->
                                                                Toast.makeText(context, "초대 링크 생성 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (e: TimeoutCancellationException) {
                                                        Toast.makeText(context, "요청 시간 초과. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
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
                                    onMenuSelect = { idx, _ -> selectedIndexHealth = idx; selectedHealthGroup = healthGroups[idx].groupSeq; },
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
                                        DropdownMenuItem(
                                            text = { Text("초대 링크 복사") },
                                            onClick = {
                                                dismiss()
                                                val groupSeq = healthGroups[selectedIndexHealth].groupSeq
                                                scope.launch {
                                                    try {
                                                        withTimeout(10000L) {
                                                            val result = groupvm.createInvitationLink(groupSeq)
                                                            result.onSuccess { inviteLink ->
                                                                clipboardManager.setText(AnnotatedString(inviteLink))
                                                                Toast.makeText(context, "초대 링크가 복사되었습니다", Toast.LENGTH_SHORT).show()
                                                            }.onFailure { error ->
                                                                Toast.makeText(context, "초대 링크 생성 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (e: TimeoutCancellationException) {
                                                        Toast.makeText(context, "요청 시간 초과. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
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
                            groupSeq = selectedCareGroup!!,
                            modifier = Modifier,
//                            vm = groupvm
                        )
                    }
                }

                composable(Route.CareSearch) {
                    val searchViewModel: com.a307.linkcare.feature.commongroup.GroupSearchViewModel = hiltViewModel()
                    GroupSearch(
                        kind = "care",
                        query = careSearchQuery,
                        onQueryChange = { careSearchQuery = it },
                        onBack = { navController.popBackStack() },
                        onJoinClick = { group ->
                            scope.launch {
                                // 케어 그룹은 선택 권한 동의 필요
                                val permissions = com.a307.linkcare.feature.commongroup.domain.PermissionAgreementDto(
                                    isSleepAllowed = true,
                                    isWaterIntakeAllowed = true,
                                    isBloodPressureAllowed = true,
                                    isBloodSugarAllowed = true
                                )
                                searchViewModel.joinGroup(group.groupSeq, permissions)
                                    .onSuccess {
                                        Toast.makeText(context, "가입 신청이 완료되었습니다", Toast.LENGTH_SHORT).show()
                                        // 검색 결과 새로고침 (상태 업데이트)
                                        if (careSearchQuery.isNotBlank()) {
                                            searchViewModel.searchGroups(careSearchQuery)
                                        }
                                        groupvm.loadCare() // 케어 그룹 목록 새로고침
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(context, "가입 신청 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
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
                    val groupSeq = groupId?.toLongOrNull() ?: return@composable
                    CareGroupEdit(
                        navController = navController,
                        groupSeq = groupSeq
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
                            groupSeq = selectedHealthGroup!!,
                            modifier = Modifier,
                            vm = groupvm
                        )
                    }
                }

                composable(Route.HealthSearch) {
                    val searchViewModel: com.a307.linkcare.feature.commongroup.GroupSearchViewModel = hiltViewModel()
                    GroupSearch(
                        kind = "health",
                        query = healthSearchQuery,
                        onQueryChange = { healthSearchQuery = it },
                        onBack = { navController.popBackStack() },
                        onJoinClick = { group ->
                            scope.launch {
                                // 헬스 그룹도 동일하게 모든 권한 동의로 처리
                                val permissions = com.a307.linkcare.feature.commongroup.domain.PermissionAgreementDto(
                                    isSleepAllowed = true,
                                    isWaterIntakeAllowed = true,
                                    isBloodPressureAllowed = true,
                                    isBloodSugarAllowed = true
                                )
                                searchViewModel.joinGroup(group.groupSeq, permissions)
                                    .onSuccess {
                                        Toast.makeText(context, "가입 신청이 완료되었습니다", Toast.LENGTH_SHORT).show()
                                        // 검색 결과 새로고침 (상태 업데이트)
                                        if (healthSearchQuery.isNotBlank()) {
                                            searchViewModel.searchGroups(healthSearchQuery)
                                        }
                                        groupvm.loadHealth() // 헬스 그룹 목록 새로고침
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(context, "가입 신청 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
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
                    val groupSeq = groupId?.toLongOrNull() ?: return@composable
                    HealthGroupEdit(
                        navController = navController,
                        groupSeq = groupSeq
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
                    val myPageViewModel: MyPageViewModel = viewModel()
                    val apiState by myPageViewModel.apiState.collectAsState()
                    val context = LocalContext.current

                    when (val state = apiState) {
                        is MyPageApiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is MyPageApiState.Success -> {
                            MyPage(
                                state = state.uiState,
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

                        is MyPageApiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "데이터를 불러오는데 실패했습니다.")
                            }
                        }
                    }
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
                        StoreItem(1, R.drawable.char_bear_1, price = 100, owned = true),
                        StoreItem(2, R.drawable.char_duck_1, price = 150, owned = false)
                    ),
                    backgroundItems = listOf(
                        StoreItem(1, R.drawable.background_1, price = 80, owned = true),
                        StoreItem(2, R.drawable.background_2, price = 120, owned = false),
                        StoreItem(3, R.drawable.background_3, price = 120, owned = false),
                        StoreItem(4, R.drawable.background_4, price = 120, owned = false),
                        StoreItem(5, R.drawable.background_5, price = 120, owned = false),
                        StoreItem(6, R.drawable.background_6, price = 120, owned = false),
                        StoreItem(7, R.drawable.background_7, price = 120, owned = false),
                        StoreItem(8, R.drawable.background_8, price = 120, owned = false)
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
                    val repo: CustomizeRepository = hiltViewModel<CustomizeRepoHolderVM>().repo
                    val context = LocalContext.current
                    val custom by repo.customFlow.collectAsState(initial = CustomEntity(1, 1))


                    DecorateScreen(
                        ownedCharacters = listOf(1, 2),
                        ownedBackgrounds = listOf(1, 2),
                        equippedCharacter = custom.characterId,
                        equippedBackground = custom.backgroundId,
                        onApply = { charId, bgId ->
                            CoroutineScope(Dispatchers.IO).launch {
                                repo.saveCustom(charId, bgId)   // 논리 ID 저장
                            }

                            DataLayerManager.sendTheme(context, charId, bgId)  // 논리 ID 전송

                            navController.goBackToMyPage()
                        }
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
