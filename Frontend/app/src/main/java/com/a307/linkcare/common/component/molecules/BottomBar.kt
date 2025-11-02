package com.a307.linkcare.common.component.molecules

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.a307.linkcare.R
import com.a307.linkcare.navigation.Route

data class BottomItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

private val bottomItems = listOf(
    BottomItem(Route.Care.route,   "케어",   R.drawable.ic_nav_care),
    BottomItem(Route.Health.route, "헬스",   R.drawable.ic_nav_health),
    BottomItem(Route.Alarm.route,  "알림",   R.drawable.ic_nav_alarm),
    BottomItem(Route.MyPage.route, "내 페이지", R.drawable.ic_nav_mypage),
)

@Composable
fun LinkCareBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        bottomItems.forEach { item ->
            val selected = currentDestination?.route == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Route.Care.route) { saveState = true }
                    }
                },
                icon = { Icon(painterResource(item.iconRes), contentDescription = item.label, modifier = Modifier.size(30.dp)) },
                label = { Text(item.label) }
            )
        }
    }
}
