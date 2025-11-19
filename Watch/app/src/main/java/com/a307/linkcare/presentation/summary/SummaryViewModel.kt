/*비즈니스로직+상태관리*/
package com.a307.linkcare.presentation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.a307.linkcare.core.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class SummaryViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val uiState =
        MutableStateFlow(
            SummaryScreenState(
                averageHeartRate =
                savedStateHandle
                    .get<Float>(Screen.Summary.averageHeartRateArg)!!
                    .toDouble(),
                totalDistance =
                savedStateHandle
                    .get<Float>(Screen.Summary.totalDistanceArg)!!
                    .toDouble(),
                totalCalories =
                savedStateHandle
                    .get<Float>(Screen.Summary.totalCaloriesArg)!!
                    .toDouble(),
                elapsedTime =
                Duration.parse(
                    savedStateHandle.get(Screen.Summary.elapsedTimeArg)!!
                )
            )
        )
}
