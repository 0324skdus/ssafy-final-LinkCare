package com.a307.linkcare.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.a307.linkcare.LinkCareApp
import com.a307.linkcare.feature.auth.IntroScreen
import com.a307.linkcare.feature.auth.LoginScreen
import com.a307.linkcare.feature.auth.SignupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Route.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
                onSignupClick = {
                    navController.navigate(Route.Signup.route)
                }
            )
        }

        // 회원가입 화면
        composable(Route.Signup.route) {
            SignupScreen(
                onVerifyEmail = { email ->
                    // TODO: 이메일 인증 로직
                },
                onSubmit = { email, password ->
                    // TODO: 회원가입 완료 처리 후 로그인 화면으로 돌아가기
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Signup.route) { inclusive = true }
                    }
                }
            )
        }

        // 인트로
        composable(Route.Intro.route) {
            IntroScreen(
                onDone = {
                    // 인트로 완료 후 메인으로
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Intro.route) { inclusive = true }
                    }
                }
            )
        }

        // 메인
        composable(Route.Main.route) {
//            LinkCareApp()
            IntroScreen()

        }
    }
}
