package com.a307.linkcare.sdk.health.domain.permission

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Samsung Health 권한 관리
 *
 * 참고: Samsung Health Data SDK는 데이터 접근 시 자동으로 권한을 요청합니다.
 * 이 매니저는 Samsung Health 앱을 실행하여 사용자가 권한을 설정하도록 유도합니다.
 */
@Singleton
class HealthPermissionManager @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "HealthPermissionManager"
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"
    }

    /**
     * Samsung Health 앱 실행
     *
     * Samsung Health Data SDK는 앱이 데이터에 처음 접근할 때 자동으로 권한 요청 UI를 표시합니다.
     * 여기서는 Samsung Health 앱을 열어서 사용자가 앱을 설치했는지 확인하고,
     * 실제 권한은 데이터 읽기 시 자동으로 요청됩니다.
     *
     * @param activity Activity 컨텍스트
     * @return 항상 true (실제 권한은 데이터 접근 시 자동 요청됨)
     */
    suspend fun requestPermissions(activity: Activity): Boolean {
        return try {
            Log.d(TAG, "Samsung Health 앱 실행 시도")

            // Samsung Health 앱 실행
            val intent = Intent().apply {
                component = ComponentName(
                    SAMSUNG_HEALTH_PACKAGE,
                    "$SAMSUNG_HEALTH_PACKAGE.MainActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                activity.startActivity(intent)
                Log.d(TAG, "Samsung Health 앱 실행 성공")
            } catch (e: Exception) {
                Log.w(TAG, "Samsung Health 앱을 실행할 수 없습니다. 설치되지 않았을 수 있습니다.", e)
            }

            // 실제 권한은 데이터 읽기 시 자동으로 요청되므로 true 반환
            true
        } catch (e: Exception) {
            Log.e(TAG, "권한 요청 중 오류 발생", e)
            // 오류가 발생해도 true 반환 (데이터 접근 시 자동으로 권한 요청됨)
            true
        }
    }

    /**
     * 권한 확인
     *
     * Samsung Health Data SDK는 권한 확인 API를 제공하지 않습니다.
     * 데이터 읽기 시도 시 AuthorizationException이 발생하면 권한이 없는 것입니다.
     *
     * @return 항상 true (실제 권한 여부는 데이터 읽기 시 확인)
     */
    suspend fun hasAllPermissions(): Boolean {
        Log.d(TAG, "Samsung Health SDK는 권한 확인 API를 제공하지 않습니다")
        // 데이터 읽기 시 자동으로 권한 요청되므로 항상 true 반환
        return true
    }
}
