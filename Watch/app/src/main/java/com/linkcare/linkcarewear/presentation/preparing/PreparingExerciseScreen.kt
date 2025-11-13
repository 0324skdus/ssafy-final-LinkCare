package com.linkcare.linkcarewear.presentation.preparing

import android.content.ContentValues.TAG
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.health.services.client.data.LocationAvailability
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
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
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
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
    val location = (uiState as? PreparingScreenState.Preparing)?.locationAvailability
    val context = LocalContext.current
    val characterId = remember { ThemePreferences.getCharacterId(context) }
    val backgroundId = remember { ThemePreferences.getBackgroundId(context) }

    ScreenScaffold(
        timeText = {
            LocationStatusText(
                updatePrepareLocationStatus(
                    locationAvailability = location ?: LocationAvailability.UNAVAILABLE
                )
            )
//            CurvedLayout(
//                anchor = 0f, // 오른쪽
//                anchorType = AnchorType.Center
//            ) {
//                curvedText(
//                    text = "1234 Steps",  // 🔹실제 값 연결
//                    fontSize = 12.sp,
//                    color = Color.DarkGray
//                )
//            }
        },
        modifier = Modifier.ambientGray(ambientState)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val backgroundRes = when (backgroundId) {
                2001L -> R.drawable.background_default
                2002L -> R.drawable.background_rock
                else -> R.drawable.background_default
            }
            // 배경 이미지
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = "배경",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            val charracterRes = when (characterId) {
                1001L -> R.drawable.duck_walk1
                1002L -> R.drawable.bear_walk1
                else -> R.drawable.duck_walk1
            }
            Image(
                painter = painterResource(id = charracterRes),
                contentDescription = "캐릭터",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(1.0f)
                    .scale(1.5f)
                    .offset(x = 10.dp, y = 8.dp)
            )

            // 내용 (텍스트 + 버튼)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 하단 시작 버튼
                FilledIconButton(
                    onClick = onStart,
                    enabled = uiState is PreparingScreenState.Preparing
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(id = R.string.start)
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
