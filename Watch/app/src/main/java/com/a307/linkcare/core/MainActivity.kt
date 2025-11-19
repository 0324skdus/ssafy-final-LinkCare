package com.a307.linkcare.core

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.a307.linkcare.data.DataLayerManager
import com.a307.linkcare.presentation.LinkCareExerciseApp
import com.a307.linkcare.presentation.exercise.ExerciseViewModel
import com.a307.linkcare.presentation.preparing.PreparingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private lateinit var navController: NavHostController
    private val exerciseViewModel by viewModels<ExerciseViewModel>()
    private val preparingViewModel by viewModels<PreparingViewModel>()

    // Register the permissions callback
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d("MainActivity", "모든 권한 승인됨")
        } else {
            Log.w("MainActivity", "일부 권한 거부됨")
        }
    }

    //초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var pendingNavigation = true

        splash.setKeepOnScreenCondition { pendingNavigation }

        super.onCreate(savedInstanceState)
        DataLayerManager.init(this)

        // Request permissions when activity is created
        requestPermissions.launch(PreparingViewModel.permissions.toTypedArray())

        setContent {
            navController = rememberSwipeDismissableNavController()

            LinkCareExerciseApp(
                navController,
                onFinishActivity = { this.finish() }
            )

            LaunchedEffect(Unit) {
                prepareIfNoExercise()
                pendingNavigation = false
            }
        }
    }

    private suspend fun prepareIfNoExercise() {
        /** Check if we have an active exercise. If true, set our destination as the
         * Exercise Screen. If false, route to preparing a new exercise. **/
        val isRegularLaunch =
            navController.currentDestination?.route == Screen.Exercise.route
        if (isRegularLaunch && !exerciseViewModel.isExerciseInProgress()) {
            navController.navigate(Screen.PreparingExercise.route)
        }
    }
}
