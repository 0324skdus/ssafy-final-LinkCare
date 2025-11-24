package com.a307.linkcare.feature.mypage.ui.decorate

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.a307.linkcare.feature.mypage.ui.mypage.MyPageViewModel
import com.a307.linkcare.navigation.Route

@Composable
fun DecorateRoute(navController: NavHostController) {
    val vm: DecorateViewModel = hiltViewModel()
    val myPageViewModel: MyPageViewModel = hiltViewModel()
    val ui by vm.uiState.collectAsState()
    val context = LocalContext.current

    DecorateScreen(
        ownedCharacters = ui.ownedCharacters,
        ownedBackgrounds = ui.ownedBackgrounds,
        equippedCharacter = ui.mainCharacter,
        equippedBackground = ui.mainBackground,
        onApply = { charId, bgId ->
            vm.applyDecoration(charId, bgId)   // 서버 저장
            vm.sendToWear(context, charId, bgId) // Wear 전송
            myPageViewModel.fetchMyPageData()
            navController.navigate(Route.MyPage.route) {
                popUpTo(Route.MyPage.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
}

fun NavController.goBackToMyPage() {
    this.navigate(Route.MyPage.route) {
        popUpTo(Route.MyPage.route) { inclusive = true }
        launchSingleTop = true
    }
}