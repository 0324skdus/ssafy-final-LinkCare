@file:OptIn(ExperimentalHorologistApi::class)

package com.linkcare.linkcarewear.presentation.exercise

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.linkcare.linkcarewear.R
import com.linkcare.linkcarewear.data.ServiceState
import com.linkcare.linkcarewear.presentation.component.CaloriesText
import com.linkcare.linkcarewear.presentation.component.HRText
import com.linkcare.linkcarewear.presentation.component.PauseButton
import com.linkcare.linkcarewear.presentation.component.ResumeButton
import com.linkcare.linkcarewear.presentation.component.StartButton
import com.linkcare.linkcarewear.presentation.component.StopButton
import com.linkcare.linkcarewear.presentation.component.formatElapsedTime
import com.linkcare.linkcarewear.presentation.dialogs.ExerciseGoalMet
import com.linkcare.linkcarewear.presentation.summary.SummaryScreenState
import com.linkcare.linkcarewear.presentation.theme.ThemePreview
import com.linkcare.linkcarewear.service.ExerciseServiceState
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.health.composables.ActiveDurationText
import com.linkcare.linkcarewear.data.DataLayerManager
import com.linkcare.linkcarewear.presentation.summary.toWorkoutSummary
import kotlinx.coroutines.launch

@Composable
fun ExerciseRoute(
    modifier: Modifier = Modifier,
    onSummary: (SummaryScreenState) -> Unit,
    onRestart: () -> Unit,
    onFinishActivity: () -> Unit
) {
    val viewModel = hiltViewModel<ExerciseViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isEnded) {
        SideEffect {
            val summary = uiState.toSummary(uiState.sessionId)
            onSummary(summary)
            DataLayerManager.sendWorkoutSummary(context, summary.toWorkoutSummary())
        }
    }

    if (uiState.error != null) {
        ErrorStartingExerciseScreen(
            onRestart = onRestart,
            onFinishActivity = onFinishActivity,
            uiState = uiState
        )
    } else {
        AmbientAware { ambientState ->
            ExerciseScreen(
                ambientState = ambientState,
                onPauseClick = { viewModel.pauseExercise() },
                onEndClick = { viewModel.endExercise() },
                onResumeClick = { viewModel.resumeExercise() },
                onStartClick = { viewModel.startExercise() },
                uiState = uiState,
                modifier = modifier
            )
        }
    }
}

/**
 * Shows an error that occurred when starting an exercise
 */
@Composable
fun ErrorStartingExerciseScreen(
    onRestart: () -> Unit,
    onFinishActivity: () -> Unit,
    uiState: ExerciseScreenState
) {
    AlertDialog(
        title = { Text(stringResource(id = R.string.error_starting_exercise)) },
        text = {
            "${uiState.error ?: Text(stringResource(id = R.string.unknown_error))}. ${
                Text(
                    stringResource(
                        id = R.string.try_again
                    )
                )
            }"
        },
        onDismissRequest = onFinishActivity,
        visible = true,
        confirmButton = {
            Button(
                onClick = onRestart
            ) {
                Text(stringResource(id = R.string.yes))
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onFinishActivity
            ) {
                Text(stringResource(id = R.string.no))
            }
        }
    )
}

/**
 * Shows while an exercise is in progress
 */
@Composable
fun ExerciseScreen(
    ambientState: AmbientState,
    onPauseClick: () -> Unit,
    onEndClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStartClick: () -> Unit,
    uiState: ExerciseScreenState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 1, pageCount = { 2 })

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState
        ) { page ->
            ScreenScaffold {
                    if (page == 0) {
                        ExerciseControlButtons(
                            uiState = uiState,
                            onStartClick = onStartClick,
                            onEndClick = onEndClick,
                            onResumeClick = {
                                onResumeClick()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onPauseClick = {
                                onPauseClick()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )
                    } else {
                        ExerciseMetrics(uiState = uiState)
                    }

            }
        }

        // If we meet an exercise goal, show our exercise met dialog.
        // This approach is for the sample, and doesn't guarantee processing of this event in all cases,
        // such as the user exiting the app while this is in-progress. Consider alternatives to exposing
        // state in a production app.
        uiState.exerciseState?.exerciseGoal?.let {
            Log.d("ExerciseGoalMet", "Showing exercise goal met dialog")
            ExerciseGoalMet(it.isNotEmpty())
        }
    }
}

@Composable
private fun ExerciseMetrics(
    uiState: ExerciseScreenState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 상단 중앙에 큰 타이머
        DurationRow(uiState)

        Spacer(modifier = Modifier.height(14.dp))

        // 하단 한 줄: 심박수 + 칼로리
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌우 두 칸을 동일한 비율로 분할
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HeartRateColumn(uiState)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CaloriesColumn(uiState)
            }
        }
    }
}

@Composable
private fun ExerciseControlButtons(
    uiState: ExerciseScreenState,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isEnding) {
                StartButton(onStartClick)
            } else {
                StopButton(onEndClick)
            }

            if (uiState.isPaused) {
                ResumeButton(onResumeClick)
            } else {
                PauseButton(onPauseClick)
            }
        }
    }
}
//현재 이동거리(미사용)
//@Composable
//private fun DistanceAndLapsRow(uiState: ExerciseScreenState) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceAround
//    ) {
//        Row {
//            DistanceText(uiState.exerciseState?.exerciseMetrics?.distance)
//        }
//    }
//}

@Composable
private fun HeartRateColumn(uiState: ExerciseScreenState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HRText(
            hr = uiState.exerciseState?.exerciseMetrics?.heartRate
        )
    }
}

@Composable
private fun CaloriesColumn(uiState: ExerciseScreenState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CaloriesText(
            uiState.exerciseState?.exerciseMetrics?.calories
        )
    }
}

@Composable
private fun DurationRow(uiState: ExerciseScreenState) {
    val checkpoint = uiState.exerciseState?.activeDurationCheckpoint
    val state = uiState.exerciseState?.exerciseState
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (checkpoint != null && state != null) {
            ActiveDurationText(checkpoint = checkpoint, state = state) {
                Text(
                    text = formatElapsedTime(it, includeSeconds = true),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text("--:--", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@WearPreviewDevices
@Composable
fun ExerciseScreenPreview() {
    ThemePreview {
        ExerciseScreen(
            onPauseClick = {},
            onEndClick = {},
            onResumeClick = {},
            onStartClick = {},
            uiState = ExerciseScreenState(
                hasExerciseCapabilities = true,
                isTrackingAnotherExercise = false,
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                exerciseState = ExerciseServiceState()
            ),
            ambientState = AmbientState.Interactive
        )
    }
}

@WearPreviewDevices
@Composable
fun ErrorStartingExerciseScreenPreview() {
    ThemePreview {
        ErrorStartingExerciseScreen(
            onRestart = {},
            onFinishActivity = {},
            uiState = ExerciseScreenState(
                hasExerciseCapabilities = true,
                isTrackingAnotherExercise = false,
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                exerciseState = ExerciseServiceState()
            )
        )
    }
}

@WearPreviewDevices
@Composable
fun ExerciseControlButtonsPreview() {
    ThemePreview {
        ExerciseControlButtons(
            uiState = ExerciseScreenState(
                hasExerciseCapabilities = true,
                isTrackingAnotherExercise = false,
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                exerciseState = ExerciseServiceState()
            ),
            onStartClick = {},
            onEndClick = {},
            onResumeClick = {},
            onPauseClick = {}
        )
    }
}
