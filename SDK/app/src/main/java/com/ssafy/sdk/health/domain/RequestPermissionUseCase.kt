package com.ssafy.sdk.health.domain

import android.app.Activity
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import javax.inject.Inject

class RequestPermissionsUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(activity: Activity): Boolean {
        return try {
            val result = healthDataStore.requestPermissions(Permissions.PERMISSIONS, activity)
            result.containsAll(Permissions.PERMISSIONS)
        } catch (e: HealthDataException) {
            throw e
        }
    }
}