package com.a307.linkcare.navigation

sealed class Route(val route: String) {
    // Auth 영역
    object Intro  : Route("intro")
    object Login  : Route("login")
    object Signup : Route("signup")
    object Onboarding : Route("onboarding")

    // Intro 영역
    object IntroTour : Route("intro_tour")

    // 탭을 감싸는 쉘
    object Main  : Route("main")

    // Main 탭
    object Care    : Route("care")
    object Health  : Route("health")
    object Alarm   : Route("alarm")
    object MyPage  : Route("mypage")

    // 새 그룹 생성 페이지
    object CreateCareGroup : Route("create_care_group")
    object CreateHealthGroup : Route("create_health_group")
}
