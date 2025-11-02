package com.ssafy.linkcaretest

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import java.security.MessageDigest

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 카카오 SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // KeyHash 출력 (카카오 개발자 콘솔에 등록 필요)
        getKeyHash()
    }

    private fun getKeyHash() {
        try {
            val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.d("KeyHash", "KeyHash: $keyHash")
                println("========================================")
                println("KeyHash: $keyHash")
                println("========================================")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "Error getting KeyHash", e)
        }
    }
}
