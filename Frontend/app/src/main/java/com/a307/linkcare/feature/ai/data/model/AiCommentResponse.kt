package com.a307.linkcare.feature.ai.data.model

import com.google.gson.annotations.SerializedName

/**
 * AI 코멘트 저장 응답
 * POST /api/ai/comment
 */
data class AiCommentResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: AiCommentData?
)

data class AiCommentData(
    @SerializedName("userSeq")
    val userSeq: Long,

    @SerializedName("groupSeq")
    val groupSeq: Long,

    @SerializedName("comment")
    val comment: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)
