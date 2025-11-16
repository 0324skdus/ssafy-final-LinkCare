package com.a307.linkcare.feature.workout.data.remote

import com.a307.linkcare.feature.workout.data.dto.WorkoutSummaryRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface WorkoutApi {
    @POST("postSomething!")  // TODO: 실제 URL로 교체
    suspend fun uploadSummary(
        @Body body: WorkoutSummaryRequest
    )
}
