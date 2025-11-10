@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.a307.linkcare.feature.auth.ui.IntroScreen
import com.a307.linkcare.feature.auth.ui.LoginScreen
import com.a307.linkcare.feature.auth.ui.SignupBirthScreen
import com.a307.linkcare.feature.auth.ui.SignupCharacterScreen
import com.a307.linkcare.feature.auth.ui.SignupGenderScreen
import com.a307.linkcare.feature.auth.ui.SignupHeightScreen
import com.a307.linkcare.feature.auth.ui.SignupNameScreen
import com.a307.linkcare.feature.auth.ui.SignupScreen
import com.a307.linkcare.feature.intro.IntroTourScreen
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
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
                navController = navController,
                onLoginSuccess = { isFirstLogin ->
                    if (isFirstLogin) {
                        // 첫 로그인 시: Intro로 이동
                        navController.navigate(Route.Intro.route) {
                            popUpTo(Route.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // 기존 사용자: 바로 메인으로 이동
                        navController.navigate(Route.Main.route) {
                            popUpTo(Route.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onSignupClick = {
                    // 회원가입 버튼 클릭 시
                    navController.navigate(Route.Signup.route)
                }
            )
        }

        // 회원가입 화면
        composable(Route.Signup.route) {
            SignupScreen(
                onVerifyEmail = { _ -> /* TODO: 이메일 인증 로직 */ },
                onSubmit = { _, _ ->
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Signup.route) { inclusive = true }
                    }
                }
            )
        }

        // 인트로 화면 (2초 후 자동으로 Onboarding 이동)
        composable(Route.Intro.route) {
            var navigated by remember { mutableStateOf(false) }

            IntroScreen(
                onDone = {
                    if (!navigated) {
                        navigated = true
                        navController.navigate(Route.Onboarding.route) {
                            popUpTo(Route.Intro.route) { inclusive = true }
                        }
                    }
                }
            )

            // 2초 뒤 자동 이동
            LaunchedEffect(Unit) {
                delay(2000)
                if (!navigated) {
                    navigated = true
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(Route.Intro.route) { inclusive = true }
                    }
                }
            }
        }

        // 온보딩 (회원정보 수집 Flow)
        composable(Route.Onboarding.route) {
            val pagerCount = 5
            val pagerState = rememberPagerState(initialPage = 0) { pagerCount }

//            val vm: OnboardingViewModel = viewModel()

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> SignupNameScreen(pagerState) { }
                    1 -> SignupGenderScreen(pagerState, pageCount = pagerCount) { }
                    2 -> SignupBirthScreen(pagerState, pageCount = pagerCount) { }
                    3 -> SignupHeightScreen(pagerState, pageCount = pagerCount) { }
                    4 -> SignupCharacterScreen(
                        pagerState = pagerState,
                        pageCount = pagerCount,
                        onStart = { idx, _ -> /* vm.setCharacter(idx) */ },
                        onNavigateToIntro = {
                            navController.navigate(Route.IntroTour.route) {
                                popUpTo(Route.Onboarding.route) { inclusive = true } // 온보딩 스택 제거
                            }
                        }
                    )
                }
            }
        }

        composable(Route.IntroTour.route) {
            IntroTourScreen(
                onDone = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.IntroTour.route) { inclusive = true }
                    }
                }
            )
        }

        // 메인 화면
        composable(Route.Main.route) {
            MainTabs(
            )
        }
    }
}

fun Route.withArgs(vararg args: Any?): String {
    var path = route
    args.forEach { arg ->
        path = path.replaceFirst("\\{[^/]+\\}".toRegex(), arg.toString())
    }
    return path
}