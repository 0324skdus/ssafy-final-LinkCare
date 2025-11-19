package com.a307.linkcare.feature.ai.data.model

import com.google.gson.annotations.SerializedName

/**
 * 사용자 건강 통계 응답
 * GET /api/health/dialogs/{userseq}/stats/today
 */
data class UserHealthStatsResponse(
    @SerializedName("totalSteps")
    val totalSteps: Int = 0,

    @SerializedName("totalDistances")
    val totalDistances: Double = 0.0,

    @SerializedName("totalDuration")
    val totalDuration: Int = 0,

    @SerializedName("totalCalories")
    val totalCalories: Double = 0.0,

    @SerializedName("avgHeartRates")
    val avgHeartRates: Double = 0.0,

    @SerializedName("sleepDuration")
    val sleepDuration: Int = 0,

    @SerializedName("totalWaterIntakes")
    val totalWaterIntakes: Double = 0.0,

    @SerializedName("lastBloodPressure")
    val lastBloodPressure: String? = null
)
