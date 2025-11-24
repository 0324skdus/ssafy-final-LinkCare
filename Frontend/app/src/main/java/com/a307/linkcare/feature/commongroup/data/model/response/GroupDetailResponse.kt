package com.a307.linkcare.feature.commongroup.data.model.response

import com.google.gson.annotations.SerializedName

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
    val members: List<GroupMemberResponse>,
    val currentUserSeq: Long  // 현재 로그인한 사용자의 userSeq
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

data class GoalCriteriaDto(
    @SerializedName("isSleepAllowed") val isSleepAllowed: Boolean?,
    @SerializedName("isWaterIntakeAllowed") val isWaterIntakeAllowed: Boolean?,
    @SerializedName("isBloodPressureAllowed") val isBloodPressureAllowed: Boolean?,
    @SerializedName("isBloodSugarAllowed") val isBloodSugarAllowed: Boolean?
)
