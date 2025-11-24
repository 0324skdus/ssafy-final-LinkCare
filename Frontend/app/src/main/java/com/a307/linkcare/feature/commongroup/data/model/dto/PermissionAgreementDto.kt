package com.a307.linkcare.feature.commongroup.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * 그룹 참가 신청 시 사용자가 동의한 선택 권한 정보
 * 필수 권한(걸음수, 심박수, 운동)은 자동으로 true이므로 포함하지 않음
 */
data class PermissionAgreementDto(
    @SerializedName("isSleepAllowed")
    val isSleepAllowed: Boolean = false,

    @SerializedName("isWaterIntakeAllowed")
    val isWaterIntakeAllowed: Boolean = false,

    @SerializedName("isBloodPressureAllowed")
    val isBloodPressureAllowed: Boolean = false,

    @SerializedName("isBloodSugarAllowed")
    val isBloodSugarAllowed: Boolean = false
)