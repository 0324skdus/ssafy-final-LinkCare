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

    // Care 하위 탭
    object CreateCareGroup : Route("care/create_care_group")
    object CareGroupHome : Route("care/care_group_home")

    // Health 하위 탭
    object CreateHealthGroup : Route("health/create_health_group")
    object HealthGroupHome : Route("health/health_group_home")

}
