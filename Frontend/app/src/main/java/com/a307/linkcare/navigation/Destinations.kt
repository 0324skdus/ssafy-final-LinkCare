package com.a307.linkcare.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

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

    // alarm 하위 탭
    object Nofitication : Route("alarm/nofitication")

    // My Page 하위 탭
    object MyPageMain : Route("mypage/my_page_main")
    object MyGroups : Route("mypage/my_groups")
    object Decorate : Route("mypage/decorate")
    object Store : Route("mypage/store")
    object EditProfile : Route("mypage/edit_profile")

    companion object {
        const val CareSearch = "care/search"
        const val HealthSearch = "health/search"
    }

    /** 초대 링크 미리보기 */
    object InvitationPreview : Route("invitation/{token}") {
        const val ARG_TOKEN = "token"

        val arguments = listOf(
            navArgument(ARG_TOKEN) { type = NavType.StringType }
        )

        fun withArgs(token: String) = "invitation/$token"
    }

    /** 그룹 수정 (id 인자 전달용) */
    object EditCareGroup : Route("care/edit_care_group/{groupId}") {
        const val ARG_GROUP_ID = "groupId"

        val arguments = listOf(
            navArgument(ARG_GROUP_ID) { type = NavType.StringType }
        )
    }
    object EditHealthGroup : Route("health/edit_health_group/{groupId}") {
        const val ARG_GROUP_ID = "groupId"

        val arguments = listOf(
            navArgument(ARG_GROUP_ID) { type = NavType.StringType }
        )
    }

}
