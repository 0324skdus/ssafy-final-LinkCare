package com.a307.linkcare.feature.commongroup.data.model.response

import com.google.gson.annotations.SerializedName

data class InvitationPreviewResponse(
    @SerializedName("groupSeq")
    val groupSeq: Long,

    @SerializedName("groupName")
    val groupName: String,

    @SerializedName("groupDescription")
    val groupDescription: String,

    @SerializedName("type")
    val type: String, // "HEALTH" or "CARE"

    @SerializedName("capacity")
    val capacity: Int,

    @SerializedName("currentMembers")
    val currentMembers: Int,

    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("isExpired")
    val isExpired: Boolean,

    @SerializedName("isFull")
    val isFull: Boolean,

    @SerializedName("invitationToken")
    val invitationToken: String,

    @SerializedName("requiredPermissions")
    val requiredPermissions: RequiredPermissions,

    @SerializedName("optionalPermissions")
    val optionalPermissions: OptionalPermissions?
) {
    data class RequiredPermissions(
        @SerializedName("isDailyStepAllowed")
        val isDailyStepAllowed: Boolean = true,

        @SerializedName("isHeartRateAllowed")
        val isHeartRateAllowed: Boolean = true,

        @SerializedName("isExerciseAllowed")
        val isExerciseAllowed: Boolean = true
    )

    data class OptionalPermissions(
        @SerializedName("isSleepAllowed")
        val isSleepAllowed: Boolean? = null,

        @SerializedName("isWaterIntakeAllowed")
        val isWaterIntakeAllowed: Boolean? = null,

        @SerializedName("isBloodPressureAllowed")
        val isBloodPressureAllowed: Boolean? = null,

        @SerializedName("isBloodSugarAllowed")
        val isBloodSugarAllowed: Boolean? = null
    )
}
