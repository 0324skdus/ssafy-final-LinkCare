package com.a307.linkcare.feature.healthgroup.data.model.response

import com.google.gson.annotations.SerializedName

data class DailyActivitySummaryResponse(
    @SerializedName("exercises")
    val exercises: List<ExerciseSessionResponse> = emptyList(),

    @SerializedName("steps")
    val steps: Int = 0
)

data class ExerciseSessionResponse(
    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("endTime")
    val endTime: String = "",

    @SerializedName("distance")
    val distance: Float = 0f,

    @SerializedName("calories")
    val calories: Float = 0f,

    @SerializedName("meanPulseRate")
    val meanPulseRate: Float = 0f,

    @SerializedName("duration")
    val duration: Long = 0
)
