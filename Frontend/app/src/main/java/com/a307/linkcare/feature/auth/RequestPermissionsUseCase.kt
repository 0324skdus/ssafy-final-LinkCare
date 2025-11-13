package com.a307.linkcare.feature.auth

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import com.a307.linkcare.feature.auth.data.HealthPermissions
import javax.inject.Inject

class RequestPermissionsUseCase @Inject constructor(
    private val client: HealthConnectClient
) {
    /**
     * 현재 부여되지 않은(요청이 필요한) 권한 Set을 돌려준다.
     * 실제 요청(launcher.launch(...))은 Compose 화면에서 수행해야 한다.
     */
    suspend operator fun invoke(): Set<String> {
        val granted: Set<String> = client.permissionController.getGrantedPermissions()
        return HealthPermissions.permissions - granted
    }
}
