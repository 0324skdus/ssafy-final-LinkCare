package com.a307.linkcare.feature.workout.data.remote

import com.a307.linkcare.feature.workout.data.dto.WorkoutSummaryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WorkoutApi {

    @POST("api/health/watch/sync")
    suspend fun uploadSummary(
        @Body body: WorkoutSummaryRequest,
        @Header("userSeq") userSeq: Int
    ): Response<Void>
}
