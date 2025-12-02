@file:OptIn(ExperimentalHorologistApi::class)

package com.a307.linkcare.presentation.exercise

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.a307.linkcare.R
import com.a307.linkcare.data.ServiceState
import com.a307.linkcare.presentation.component.CaloriesText
import com.a307.linkcare.presentation.component.HRText
import com.a307.linkcare.presentation.component.PauseButton
import com.a307.linkcare.presentation.component.ResumeButton
import com.a307.linkcare.presentation.component.StartButton
import com.a307.linkcare.presentation.component.StopButton
import com.a307.linkcare.presentation.component.formatElapsedTime
import com.a307.linkcare.presentation.dialogs.ExerciseGoalMet
import com.a307.linkcare.presentation.summary.SummaryScreenState
import com.a307.linkcare.presentation.theme.ThemePreview
import com.a307.linkcare.service.ExerciseServiceState
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.android.horologist.health.composables.ActiveDurationText
import com.a307.linkcare.core.Constants
import com.a307.linkcare.data.DataLayerManager
import com.a307.linkcare.data.ThemePreferences
import com.a307.linkcare.presentation.summary.toWorkoutSummary
import kotlinx.coroutines.delay
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

    // 화면 로드 시 초기 상태를 기록하여 이전 세션의 isEnded 무시
    val initialIsEnded = remember { uiState.isEnded }
    var hasNavigatedToSummary by remember { mutableStateOf(false) }

    // 운동이 종료되면 Summary 화면으로 이동 (초기 isEnded 상태면 무시)
    if (uiState.isEnded && !initialIsEnded && !hasNavigatedToSummary) {
        hasNavigatedToSummary = true
        SideEffect {
            val summary = uiState.toSummary(uiState.sessionId)
            onSummary(summary)
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 🔹 페이지 전환 (0=제어, 1=운동 데이터)
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> ExerciseControlButtons(
                    uiState = uiState,
                    onStartClick = onStartClick,
                    onEndClick = onEndClick,
                    onResumeClick = {
                        onResumeClick()
                        coroutineScope.launch { pagerState.scrollToPage(1) }
                    },
                    onPauseClick = {
                        onPauseClick()
                        coroutineScope.launch { pagerState.scrollToPage(1) }
                    }
                )

                1 -> ExerciseMetrics(uiState = uiState)
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
    val context = LocalContext.current

    // 1. 상태 관리 (remember → mutableState)
    var characterId by remember { mutableStateOf(ThemePreferences.getCharacterId(context)) }
    var backgroundId by remember { mutableStateOf(ThemePreferences.getBackgroundId(context)) }

    // 2. SharedPreferences Listener 등록
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("customize_prefs", Context.MODE_PRIVATE)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "characterId" -> characterId = prefs.getInt("characterId", 1)
                "backgroundId" -> backgroundId = prefs.getInt("backgroundId", 1)
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 3. Int 기반 리소스 매핑
        val backgroundRes = when (backgroundId) {
            1 -> R.drawable.background_default
            2 -> R.drawable.background_2
            3 -> R.drawable.background_3
            4 -> R.drawable.background_4
            5 -> R.drawable.background_5
            6 -> R.drawable.background_6
            7 -> R.drawable.background_7
            8 -> R.drawable.background_8
            else -> R.drawable.background_default
        }

        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "배경",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        AnimatedCharacter(
            characterId = characterId,
            isAnimating = !uiState.isPaused,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Constants.UI.CHARACTER_BOTTOM_PADDING_DP.dp)
                .scale(Constants.UI.CHARACTER_SCALE)
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            DurationRow(uiState)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    HeartRateColumn(uiState)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CaloriesColumn(uiState)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun HeartRateColumn(uiState: ExerciseScreenState) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        HRText(
            hr = uiState.exerciseState?.exerciseMetrics?.heartRate,
        )
    }
}

@Composable
private fun CaloriesColumn(uiState: ExerciseScreenState) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CaloriesText(
            uiState.exerciseState?.exerciseMetrics?.calories,
        )
    }
}

@Composable
private fun DurationRow(uiState: ExerciseScreenState) {
    val checkpoint = uiState.exerciseState?.activeDurationCheckpoint
    val state = uiState.exerciseState?.exerciseState

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.White.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp) // 글자 뒤 최소 패딩
        ) {
            if (checkpoint != null && state != null) {
                ActiveDurationText(checkpoint = checkpoint, state = state) {
                    Text(
                        text = formatElapsedTime(it, includeSeconds = true),
                        fontSize = 38.sp, // 워치 화면 기준 적당히 키움
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            } else {
                Text(
                    "--:--",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
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

@Composable
fun AnimatedCharacter(
    characterId: Int,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val frames = when (characterId) {
        1 -> listOf(
            R.drawable.bear_walk1,
            R.drawable.bear_walk2,
            R.drawable.bear_walk3,
            R.drawable.bear_walk4
        )

        2 -> listOf(
            R.drawable.bear2_walk1,
            R.drawable.bear2_walk2,
            R.drawable.bear2_walk3,
            R.drawable.bear2_walk4
        )

        3 -> listOf(
            R.drawable.bear3_walk1,
            R.drawable.bear3_walk2,
            R.drawable.bear3_walk3,
            R.drawable.bear3_walk4
        )

        4 -> listOf(
            R.drawable.duck_walk1,
            R.drawable.duck_walk2,
            R.drawable.duck_walk3,
            R.drawable.duck_walk4,
            R.drawable.duck_walk5
        )

        5 -> listOf(
            R.drawable.duck2_walk1,
            R.drawable.duck2_walk2,
            R.drawable.duck2_walk3,
            R.drawable.duck2_walk4,
            R.drawable.duck2_walk5
        )
        else -> listOf(
            R.drawable.bear_walk1,
            R.drawable.bear_walk2,
            R.drawable.bear_walk3,
            R.drawable.bear_walk4
        )
    }

    var currentFrame by remember { mutableStateOf(0) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            while (true) {
                delay(Constants.UI.ANIMATION_FRAME_DELAY_MS)
                currentFrame = (currentFrame + 1) % frames.size
            }
        } else {
            currentFrame = 0
        }
    }

    Image(
        painter = painterResource(id = frames[currentFrame]),
        contentDescription = "캐릭터",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

