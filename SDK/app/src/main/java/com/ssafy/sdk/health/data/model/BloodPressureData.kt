package com.ssafy.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import java.time.LocalDateTime
import java.time.ZoneOffset

data class BloodPressureTypeData (
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val systolic: Float?,
    val diastolic: Float?,
    val mean: Float?,
    val pulseRate: Int?,
    val medicationTaken: Boolean?
)

data class BloodPressureData (
    val startTime: LocalDateTime?,
    val bloodPressures: List<BloodPressureTypeData>
)

