package com.a307.linkcare.feature.commongroup.domain

data class GroupDetailResponse(
    val groupSeq: Long,
    val groupName: String,
    val groupDescription: String,
    val type: String,
    val capacity: Int,
    val currentMembers: Int,
    val imageUrl: String,
    val createdAt: String,
    val goalCriteria: GoalCriteria?,
    val members: List<GroupMemberResponse>
)

data class GoalCriteria(
    val minCalorie: Double,
    val minStep: Int,
    val minDistance: Double,
    val minDuration: Int
)

data class GroupMemberResponse(
    val groupMemberSeq: Long,
    val userSeq: Long,
    val userName: String,
    val isLeader: Boolean,
    val mainCharacterBaseImageUrl: String
)
