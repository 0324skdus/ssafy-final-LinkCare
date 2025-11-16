package com.linkcare.linkcarewear.presentation.preparing

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.services.client.data.LocationAvailability
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.curvedText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.linkcare.linkcarewear.R
import com.linkcare.linkcarewear.data.ServiceState
import com.linkcare.linkcarewear.presentation.ambient.ambientGray
import com.linkcare.linkcarewear.presentation.dialogs.ExerciseInProgressAlert
import com.linkcare.linkcarewear.presentation.theme.ThemePreview
import com.linkcare.linkcarewear.service.ExerciseServiceState
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.linkcare.linkcarewear.data.ThemePreferences

@Composable
fun PreparingExerciseRoute(
    onStart: () -> Unit,
    onFinishActivity: () -> Unit,
    onNoExerciseCapabilities: () -> Unit,
    onGoals: () -> Unit
) {
    val viewModel = hiltViewModel<PreparingViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /** Request permissions prior to launching exercise.**/
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            Log.d(TAG, "All required permissions granted")
        }
    }

    SideEffect {
        val preparingState = uiState
        if (preparingState is PreparingScreenState.Preparing &&
            !preparingState.hasExerciseCapabilities
        ) {
            onNoExerciseCapabilities()
        }
    }

    if (uiState.serviceState is ServiceState.Connected) {
        val requiredPermissions = uiState.requiredPermissions
        LaunchedEffect(requiredPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    AmbientAware { ambientState ->
        PreparingExerciseScreen(
            onStart = {
                viewModel.startExercise()
                onStart()
            },
            uiState = uiState,
//            onGoals = { onGoals() },
            ambientState = ambientState
        )
    }

    if (uiState.isTrackingInAnotherApp) {
        var dismissed by remember { mutableStateOf(false) }
        ExerciseInProgressAlert(
            onNegative = onFinishActivity,
            onPositive = { dismissed = true },
            showDialog = !dismissed
        )
    }
}

/**
 * Screen that appears while the device is preparing the exercise.
 */
@Composable
fun PreparingExerciseScreen(
    uiState: PreparingScreenState,
    ambientState: AmbientState,
    onStart: () -> Unit = {}
) {
    val context = LocalContext.current

    // 1) 화면에서 사용할 상태
    var characterId by remember { mutableStateOf(ThemePreferences.getCharacterId(context)) }
    var backgroundId by remember { mutableStateOf(ThemePreferences.getBackgroundId(context)) }

    // 2) SharedPreferences 변경 감지 리스너 등록
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("customize_prefs", Context.MODE_PRIVATE)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "characterId" -> {
                    characterId = prefs.getInt("characterId", 1)
                }
                "backgroundId" -> {
                    backgroundId = prefs.getInt("backgroundId", 1)
                }
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        // 🔥 화면이 사라질 때 자동으로 해제
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    // 3) 화면 UI 구성부
    ScreenScaffold(
        timeText = {
            LocationStatusText(
                updatePrepareLocationStatus(
                    locationAvailability = (uiState as? PreparingScreenState.Preparing)
                        ?.locationAvailability ?: LocationAvailability.UNAVAILABLE
                )
            )
        },
        modifier = Modifier.ambientGray(ambientState)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 🔥 4) 배경 리소스 매핑 (Int 기반)
            val backgroundRes = when (backgroundId) {
                1 -> R.drawable.background_default
                2 -> R.drawable.background_2
                else -> R.drawable.background_default
            }

            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = "배경",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // 🔥 5) 캐릭터 리소스 매핑 (Int 기반)
            val characterRes = when (characterId) {
                1 -> R.drawable.duck_walk1
                2 -> R.drawable.bear_walk1
                else -> R.drawable.duck_walk1
            }

            Image(
                painter = painterResource(id = characterRes),
                contentDescription = "캐릭터",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(1.0f)
                    .scale(1.5f)
                    .offset(x = 10.dp, y = 8.dp)
            )

            // Start 버튼 등 나머지 UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledIconButton(
                    onClick = onStart,
                    enabled = uiState is PreparingScreenState.Preparing
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start"
                    )
                }
            }
        }
    }
}

/**Return [LocationAvailability] value code as a string**/

@Composable
private fun updatePrepareLocationStatus(locationAvailability: LocationAvailability): String {
    val gpsText = when (locationAvailability) {
        LocationAvailability.ACQUIRED_TETHERED, LocationAvailability.ACQUIRED_UNTETHERED
            -> R.string.GPS_acquired

        LocationAvailability.NO_GNSS -> R.string.GPS_disabled
        // TODO Consider redirecting user to change device settings in this case
        LocationAvailability.ACQUIRING -> R.string.GPS_acquiring
        LocationAvailability.UNKNOWN -> R.string.GPS_initializing
        else -> R.string.GPS_unavailable
    }

    return stringResource(id = gpsText)
}

@Composable
private fun LocationStatusText(status: String) {
    CurvedLayout {
        curvedText(
            text = status,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@WearPreviewDevices
@Composable
fun PreparingExerciseScreenPreview() {
    ThemePreview {
        PreparingExerciseScreen(
            uiState = PreparingScreenState.Preparing(
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                isTrackingInAnotherApp = false,
                requiredPermissions = PreparingViewModel.permissions,
                hasExerciseCapabilities = true
            ),
            ambientState = AmbientState.Interactive
        )
    }
}

@WearPreviewDevices
@Composable
fun PreparingExerciseScreenPreviewAmbient() {
    ThemePreview {
        PreparingExerciseScreen(
            uiState = PreparingScreenState.Preparing(
                serviceState = ServiceState.Connected(
                    ExerciseServiceState()
                ),
                isTrackingInAnotherApp = false,
                requiredPermissions = PreparingViewModel.permissions,
                hasExerciseCapabilities = true
            ),
            ambientState = AmbientState.Ambient()
        )
    }
}
