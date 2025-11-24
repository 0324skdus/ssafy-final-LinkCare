package com.a307.linkcare.feature.commongroup.data.model.response

data class MemberCommentResponse(
    val userSeq: Long,
    val userName: String,
    val comment: String?,
    val updatedAt: String?
)

data class GroupCommentsData(
    val comments: List<MemberCommentResponse>?
)

data class GroupCommentsResponse(
    val success: Boolean,
    val status: Int,
    val message: String,
    val data: GroupCommentsData?,
    val timestamp: String
)
