package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.caregroup.domain.HealthSummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface HealthFeedbackApi {

    @GET("/api/health/feedback/{userSeq}")
    suspend fun getHealthFeedback(
        @Path("userSeq") userSeq: Int
    ): Response<HealthSummaryResponse>
}
