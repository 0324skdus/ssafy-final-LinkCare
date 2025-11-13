@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.navigation

import android.util.Log
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.feature.auth.OnboardingViewModel
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
import com.a307.linkcare.feature.auth.ui.SignupScreen
import com.a307.linkcare.feature.intro.IntroTourScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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

                onSubmit = { name, email, password ->
                    scope.launch {
                        try {
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
                            // 화면 전환 등으로 코루틴이 취소된 정상 상황
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

            fun goOnce() {
                if (!navigated) {
                    navigated = true
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(Route.Intro.route) { inclusive = true }
                    }
                }
            }

            IntroScreen(onDone = { goOnce() })

            LaunchedEffect(Unit) {
                delay(2000)
                goOnce()
            }
        }

        // 온보딩 (회원정보 수집 Flow)
        composable(Route.Onboarding.route) {
            SignupPager(
                navController = navController,
                onFinished = {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
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

@Composable
fun SignupPager(
    navController: NavHostController,
    vm: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val pageCount = 4
    val pagerState = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()

    fun goNext() = scope.launch {
        val next = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
        pagerState.animateScrollToPage(next)
    }

    // 화면들
    HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
        when (page) {
            0 -> SignupGenderScreen(
                pagerState = pagerState,
                pageCount = pageCount,
                onSubmit = { gender -> vm.setGender(gender); goNext() }
            )
            1 -> SignupBirthScreen(
                pagerState = pagerState,
                pageCount = pageCount,
                onSubmit = { birth -> vm.setBirth(birth); goNext() }
            )
            2 -> SignupHeightScreen(
                pagerState = pagerState,
                pageCount = pageCount,
                onSubmit = { (h, w) -> vm.setHeight(h); vm.setWeight(w); goNext() }
            )
            3 -> SignupCharacterScreen(
                pagerState = pagerState,
                pageCount = pageCount,
                onStart = { idx, name ->
                    // 캐릭터 선택 후 프로필 제출
//                    vm.setCharacterId(idx);
//                    vm.setCharacterName(name);
//                   vm.submitProfile();
                },
                onNavigateToIntro = onFinished
            )
        }
    }

    val submit by vm.submit.collectAsState()
    var navigated by remember { mutableStateOf(false) }

    // 프로필 제출 성공 시 다음 화면으로 이동
    LaunchedEffect(submit) {
        if (!navigated && submit is OnboardingViewModel.SubmitState.Success) {
            navigated = true
            onFinished()
        }
    }

    // 로딩 및 에러 UI
    when (submit) {
        is OnboardingViewModel.SubmitState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = main)
            }
        }
        is OnboardingViewModel.SubmitState.Error -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                Toast.makeText(context, "프로필 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
        }
        else -> Unit
    }
}