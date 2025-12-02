package com.a307.linkcare.feature.caregroup.data.model.response

import com.google.gson.annotations.SerializedName

data class DailyHealthDetailResponse(
    @SerializedName("bloodPressures")
    val bloodPressures: List<BloodPressureResponse> = emptyList(),

    @SerializedName("waterIntakes")
    val waterIntakes: List<WaterIntakeResponse> = emptyList(),

    @SerializedName("sleeps")
    val sleeps: List<SleepResponse> = emptyList(),

    @SerializedName("dailyActivitySummary")
    val dailyActivitySummary: DailyActivitySummaryResponse? = null,

    @SerializedName("heartRates")
    val heartRates: List<HeartRateResponse> = emptyList()
)

data class BloodPressureResponse(
    @SerializedName("bloodPressureId")
    val bloodPressureId: Int = 0,

    @SerializedName("uid")
    val uid: String = "",

    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("systolic")
    val systolic: Float = 0f,

    @SerializedName("diastolic")
    val diastolic: Float = 0f,

    @SerializedName("mean")
    val mean: Float = 0f,

    @SerializedName("pulseRate")
    val pulseRate: Int = 0
)

data class WaterIntakeResponse(
    @SerializedName("waterIntakeId")
    val waterIntakeId: Int = 0,

    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("amount")
    val amount: Float = 0f
)

data class SleepResponse(
    @SerializedName("sleepId")
    val sleepId: Int = 0,

    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("endTime")
    val endTime: String = "",

    @SerializedName("duration")
    val duration: Int = 0
)

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

data class HeartRateResponse(
    @SerializedName("heartRateId")
    val heartRateId: Int = 0,

    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("endTime")
    val endTime: String = "",

    @SerializedName("heartRate")
    val heartRate: Double = 0.0
)
