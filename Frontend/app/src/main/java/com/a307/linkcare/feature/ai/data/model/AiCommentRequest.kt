package com.a307.linkcare.feature.ai.data.model

import com.google.gson.annotations.SerializedName

/**
 * AI 코멘트 저장 요청
 * POST /api/ai/comment
 */
data class AiCommentRequest(
    @SerializedName("groupSeq")
    val groupSeq: Long,

    @SerializedName("comment")
    val comment: String
)
