package com.a307.linkcare.navigation

sealed class Route(val route: String) {
    object Main  : Route("main")

    // Auth 영역
    object Intro  : Route("intro")
    object Login  : Route("login")
    object Signup : Route("signup")
    object Onboarding : Route("onboarding")

    // Main 탭 영역
    object Care    : Route("care")
    object Health  : Route("health")
    object Alarm   : Route("alarm")
    object MyPage  : Route("mypage")
}
