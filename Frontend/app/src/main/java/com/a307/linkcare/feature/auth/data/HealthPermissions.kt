package com.a307.linkcare.feature.auth.data

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*

object HealthPermissions {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
    )
}
