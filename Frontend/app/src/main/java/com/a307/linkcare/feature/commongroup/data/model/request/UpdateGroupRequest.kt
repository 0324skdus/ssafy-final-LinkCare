package com.a307.linkcare.feature.commongroup.data.model.request

import com.google.gson.annotations.SerializedName

data class UpdateGroupRequest(
    @SerializedName("groupName")
    val groupName: String,

    @SerializedName("groupDescription")
    val groupDescription: String,

    // 케어 그룹 전용: 선택 권한 항목
    @SerializedName("isSleepRequired")
    val isSleepRequired: Boolean? = null,

    @SerializedName("isWaterIntakeRequired")
    val isWaterIntakeRequired: Boolean? = null,

    @SerializedName("isBloodPressureRequired")
    val isBloodPressureRequired: Boolean? = null,

    @SerializedName("isBloodSugarRequired")
    val isBloodSugarRequired: Boolean? = null
)