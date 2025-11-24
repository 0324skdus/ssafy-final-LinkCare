package com.a307.linkcare.feature.workout.data.api

import com.a307.linkcare.feature.workout.domain.dto.WorkoutSummaryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WorkoutApi {

    @POST("api/health/exercise/watch/sync")
    suspend fun uploadSummary(
        @Body body: WorkoutSummaryRequest,
        @Header("userSeq") userSeq: Int
    ): Response<Void>
}
