package com.a307.linkcare.feature.caregroup.data.api

import com.a307.linkcare.feature.caregroup.data.model.response.HealthSummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.time.LocalDate

interface HealthFeedbackApi {

    @GET("/api/health/feedback/{userSeq}/{date}")
    suspend fun getHealthFeedback(
        @Path("userSeq") userSeq: Int,
        @Path("date") date: LocalDate
    ): Response<HealthSummaryResponse>
}
