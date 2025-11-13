package com.linkcare.linkcarewear.app

import androidx.navigation.NavController
import com.linkcare.linkcarewear.presentation.summary.SummaryScreenState

sealed class Screen(
    val route: String
) {
    object Exercise : Screen("exercise")

    object ExerciseNotAvailable : Screen("exerciseNotAvailable")

    object PreparingExercise : Screen("preparingExercise")

    object Goals : Screen(route = "goals")

    object Summary : Screen("summaryScreen") {
        fun buildRoute(summary: SummaryScreenState): String =
            "$route/${summary.averageHeartRate}/${summary.totalDistance}" +
                "/${summary.totalCalories}/${summary.elapsedTime}"

        val averageHeartRateArg = "averageHeartRate"
        val totalDistanceArg = "totalDistance"
        val totalCaloriesArg = "totalCalories"
        val elapsedTimeArg = "elapsedTime"
    }
}

fun NavController.navigateToTopLevel(
    screen: Screen,
    route: String = screen.route
) {
    navigate(route) {
        popUpTo(graph.id) {
            inclusive = true
        }
    }
}
