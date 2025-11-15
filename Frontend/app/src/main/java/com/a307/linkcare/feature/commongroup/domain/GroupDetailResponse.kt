package com.a307.linkcare.feature.commongroup.domain

import com.google.gson.annotations.SerializedName

data class GroupDetailResponse(
    @SerializedName("groupSeq") val groupSeq: Long,
    @SerializedName("groupName") val groupName: String,
    @SerializedName("groupDescription") val groupDescription: String,
    @SerializedName("type") val type: String,  // HEALTH / CARE
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("currentMembers") val currentMembers: Int,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("goalCriteria") val goalCriteria: GoalCriteriaDto?,
    @SerializedName("members") val members: List<MemberDto>
)

data class MemberDto(
    @SerializedName("groupMemberSeq") val groupMemberSeq: Long,
    @SerializedName("userSeq") val userSeq: Long,
    @SerializedName("userName") val userName: String,
    @SerializedName("isLeader") val isLeader: Boolean,
    @SerializedName("mainCharacterBaseImageUrl") val mainCharacterBaseImageUrl: String?
)

data class GoalCriteriaDto(
    @SerializedName("isSleepAllowed") val isSleepAllowed: Boolean?,
    @SerializedName("isWaterIntakeAllowed") val isWaterIntakeAllowed: Boolean?,
    @SerializedName("isBloodPressureAllowed") val isBloodPressureAllowed: Boolean?,
    @SerializedName("isBloodSugarAllowed") val isBloodSugarAllowed: Boolean?
)
