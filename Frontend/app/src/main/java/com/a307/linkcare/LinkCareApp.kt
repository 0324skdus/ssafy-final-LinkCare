package com.a307.linkcare

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.a307.linkcare.R
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import java.security.MessageDigest
import com.a307.linkcare.feature.auth.data.RetrofitClient
@HiltAndroidApp
class LinkCareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))
    }

    private fun printKeyHash() {
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
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "KeyHash error", e)
        }
    }
}
