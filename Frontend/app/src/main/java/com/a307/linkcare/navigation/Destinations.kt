package com.a307.linkcare.navigation

sealed class Route(val route: String) {
    object Login : Route("login")
    object Main  : Route("main")
    object Signup : Route("signup")
    object Intro : Route("intro")
}
