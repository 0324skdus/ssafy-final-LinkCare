package com.a307.linkcare.feature.ai.data.api

import com.a307.linkcare.feature.ai.data.model.AiCommentRequest
import com.a307.linkcare.feature.ai.data.model.AiCommentResponse
import com.a307.linkcare.feature.ai.data.model.UserHealthStatsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * AI 코멘트 관련 API
 */
interface AiApi {

    /**
     * AI 코멘트 저장
     * POST /api/ai/comment
     */
    @POST("/api/ai/comment")
    suspend fun postAiComment(
        @Body request: AiCommentRequest
    ): Response<AiCommentResponse>

    /**
     * 특정 사용자의 오늘 건강 통계 조회
     * GET /api/health/dialogs/{userseq}/stats/today
     */
    @GET("/api/health/dialogs/{userseq}/stats/today")
    suspend fun getUserHealthStatsToday(
        @Path("userseq") userSeq: Long
    ): Response<UserHealthStatsResponse>
}
