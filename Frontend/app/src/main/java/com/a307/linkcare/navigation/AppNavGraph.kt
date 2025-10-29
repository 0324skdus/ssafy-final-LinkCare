package com.a307.linkcare.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.a307.linkcare.feature.auth.LinkCareLoginScreen
import com.a307.linkcare.MainWithBottomBar

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Route.Login.route   // 앱 시작: 로그인 화면
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Login.route) {
            LinkCareLoginScreen()
            // 로그인 성공 시 이동
            // navController.navigate(Route.Main.route) {
            //     popUpTo(Route.Login.route) { inclusive = true }
            // }
        }

        // 메인(탭 있는 화면)
        composable(Route.Main.route) {
            MainWithBottomBar()
        }
    }
}
