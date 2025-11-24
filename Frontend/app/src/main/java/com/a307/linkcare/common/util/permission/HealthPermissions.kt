package com.a307.linkcare.common.util.permission

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

object HealthPermissions {
    val permissions: Set<String> = setOf(
        HealthPermission.Companion.getReadPermission(StepsRecord::class),
        HealthPermission.Companion.getReadPermission(HeartRateRecord::class),
        HealthPermission.Companion.getReadPermission(SleepSessionRecord::class),
        HealthPermission.Companion.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.Companion.getReadPermission(BloodPressureRecord::class),
        HealthPermission.Companion.getReadPermission(BodyWaterMassRecord::class),
    )
}