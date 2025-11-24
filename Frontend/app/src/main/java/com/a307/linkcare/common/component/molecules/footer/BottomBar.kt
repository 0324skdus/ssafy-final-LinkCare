package com.a307.linkcare.common.component.molecules.footer

import androidx.compose.material3.NavigationBarItemDefaults
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.a307.linkcare.R
import com.a307.linkcare.common.theme.*
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
fun BottomBar(
    containerColor: Color = white,
    contentColor: Color = black,
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    Column {
        Divider(
            color = Color(0xFFD9D9D9),
            thickness = 1.dp
        )

        NavigationBar (
            containerColor = containerColor,
            tonalElevation = 0.dp
        )
        {
            bottomItems.forEach { item ->
                val selected = currentDestination?.route == item.route
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                    onClick = {
                        val alreadySelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        if (!alreadySelected) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        }
                    },
                    icon = {
                        Icon(
                            painterResource(item.iconRes),
                            contentDescription = item.label,
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = main,
                        unselectedIconColor = Color(0xFF666666),
                        selectedTextColor = main,
                        unselectedTextColor = Color(0xFF666666),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

