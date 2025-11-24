package com.a307.linkcare.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.watch.domain.model.WorkoutSummary
import com.a307.linkcare.feature.workout.domain.dto.WorkoutSummaryRequest
import com.a307.linkcare.feature.workout.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutSyncViewModel @Inject constructor(
    private val repo: WorkoutRepository
) : ViewModel() {

    fun sendSummary(summary: WorkoutSummary) {
        viewModelScope.launch {
            val req = WorkoutSummaryRequest(
                sessionId = summary.sessionId,
                avgHeartRate = summary.avgHeartRate,
                calories = summary.calories,
                distance = summary.distance,
                durationSec = summary.durationSec,
                startTimestamp = summary.startTimestamp,
                endTimestamp = summary.endTimestamp
            )

            repo.uploadSummary(req)   // userSeq는 Repository가 처리
        }
    }
}
