@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.a307.linkcare.feature.auth.data.RetrofitClient
import com.a307.linkcare.feature.auth.domain.SendVerificationCodeRequest
import com.a307.linkcare.feature.auth.domain.SignupRequest
import com.a307.linkcare.feature.auth.domain.VerifyCodeRequest
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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
            // 네트워크 콜백에 필요한 Compose 도구
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

            SignupScreen(
                onVerifyEmail = { email ->
                    scope.launch {
                        try {
                            // 1) 중복 확인
                            val dupRes = RetrofitClient.authApi.checkEmail(email)
                            if (!dupRes.isSuccessful) {
                                toast("이메일 중복 확인 실패")
                                return@launch
                            }
                            val isDuplicate = dupRes.body() ?: false
                            if (isDuplicate) {
                                toast("이미 가입된 이메일입니다.")
                                return@launch
                            }

                            // 2) 사용 가능: 인증 코드 발송
                            val sendRes = RetrofitClient.authApi.sendVerificationCode(
                                SendVerificationCodeRequest(email)
                            )
                            if (sendRes.isSuccessful) {
                                toast("인증 코드를 전송했어요. 메일함을 확인해주세요.")
                            } else {
                                toast("인증 코드 발송 실패")
                            }
                        } catch (ce: CancellationException) {
                            // ignore
                        } catch (e: Exception) {
                            toast("서버 통신 오류")
                        }
                    }
                },

                onVerifyCode = { email, code, onResult ->
                    scope.launch {
                        try {
                            val res = RetrofitClient.authApi.verifyCode(
                                VerifyCodeRequest(email = email, code = code)
                            )
                            if (res.isSuccessful) {
                                toast("이메일 인증이 완료되었어요.")
                                onResult(true)
                            } else {
                                toast("인증 코드가 올바르지 않습니다.")
                                onResult(false)
                            }
                        } catch (e: Exception) {
                            toast("서버 통신 오류")
                            onResult(false)
                        }
                    }
                },

                onSubmit = { email, password ->
                    scope.launch {
                        try {
                            val name = email.substringBefore("@").ifBlank { "사용자" }
                            val res = RetrofitClient.authApi.signup(
                                SignupRequest(email = email, password = password, name = name)
                            )
                            if (res.isSuccessful) {
                                toast("회원가입이 완료되었어요. 로그인해 주세요.")
                                navController.navigate(Route.Login.route) {
                                    popUpTo(Route.Signup.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                toast("회원가입 실패")
                            }
                        } catch (ce: CancellationException) {
                            // 화면 전환 등으로 코루틴이 취소된 정상 상황 -> 에러 토스트 금지
                            // 그냥 무시 (필요시 로그만 남겨도 됨)
                        } catch (e: Exception) {
                            toast("서버 통신 오류")
                        }
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