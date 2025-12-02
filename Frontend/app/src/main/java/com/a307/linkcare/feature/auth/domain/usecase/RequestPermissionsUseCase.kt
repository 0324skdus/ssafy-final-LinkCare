package com.a307.linkcare.feature.auth.domain.usecase

import android.app.Activity
import androidx.health.connect.client.HealthConnectClient
import com.a307.linkcare.common.util.permission.HealthPermissions
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import javax.inject.Inject

class RequestPermissionsUseCase @Inject constructor(
    private val client: HealthConnectClient,
    private val healthDataStore: HealthDataStore

) {
    /**
     * 현재 부여되지 않은(요청이 필요한) 권한 Set을 돌려준다.
     * 실제 요청(launcher.launch(...))은 Compose 화면에서 수행해야 한다.
     */
    suspend operator fun invoke(): Set<String> {
        val granted: Set<String> = client.permissionController.getGrantedPermissions()
        return HealthPermissions.permissions - granted
    }

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
