/*실제화면*/
package com.a307.linkcare.presentation.summary

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.a307.linkcare.R
import com.a307.linkcare.presentation.component.SummaryFormat
import com.a307.linkcare.presentation.component.formatCalories
import com.a307.linkcare.presentation.component.formatDistanceKm
import com.a307.linkcare.presentation.component.formatElapsedTime
import com.a307.linkcare.presentation.component.formatHeartRate
import com.a307.linkcare.presentation.theme.ThemePreview
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import java.time.Duration

/**End-of-workout summary screen**/
@Composable
fun SummaryRoute(
    onRestartClick: () -> Unit
) {
    val viewModel = hiltViewModel<SummaryViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SummaryScreen(uiState = uiState, onRestartClick = onRestartClick)
}

@Composable
fun SummaryScreen(
    uiState: SummaryScreenState,
    onRestartClick: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button
    )
    ScreenScaffold(scrollState = columnState, contentPadding = contentPadding) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.workout_complete))
                }
            }
            item {
                SummaryFormat(
                    value = formatElapsedTime(uiState.elapsedTime, includeSeconds = true),
                    metric = stringResource(id = R.string.duration),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SummaryFormat(
                    value = formatHeartRate(uiState.averageHeartRate),
                    metric = stringResource(id = R.string.avgHR),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SummaryFormat(
                    value = formatDistanceKm(uiState.totalDistance),
                    metric = stringResource(id = R.string.distance),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SummaryFormat(
                    value = formatCalories(uiState.totalCalories),
                    metric = stringResource(id = R.string.calories),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    label = { Text(stringResource(id = R.string.restart)) },
                    onClick = onRestartClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A89F6),
                        contentColor = Color.White
                    )
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun SummaryScreenPreview() {
    ThemePreview {
        SummaryScreen(
            uiState = SummaryScreenState(
                averageHeartRate = 75.0,
                totalDistance = 2000.0,
                totalCalories = 100.0,
                elapsedTime = Duration.ofMinutes(17).plusSeconds(1),
                sessionId = 123
            ),
            onRestartClick = {}
        )
    }
}
