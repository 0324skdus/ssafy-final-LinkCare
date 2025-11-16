package com.a307.linkcare.sdk.health.data.model

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.device.DeviceType
import java.time.LocalDateTime
import java.time.ZoneOffset

data class HeartRatesData(
    val deviceId: String,
    val deviceType: DeviceType,
    val uid: String,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val zoneOffset: ZoneOffset?,
    val dataSource: DataSource?,
    val heartRate: Float?,
    val minHeartRate: Float?,
    val maxHeartRate: Float?
)