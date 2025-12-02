package com.a307.linkcare

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.kakao.sdk.common.KakaoSdk
import com.a307.linkcare.feature.ai.domain.lifecycle.AiCommentLifecycleObserver
import com.a307.linkcare.feature.ai.domain.worker.AiWorkManager
import com.a307.linkcare.common.network.client.RetrofitClient
import dagger.hilt.android.HiltAndroidApp
import java.security.MessageDigest
import javax.inject.Inject

@HiltAndroidApp
class LinkCareApp : Application(), Configuration.Provider {

    @Inject
    lateinit var aiWorkManager: AiWorkManager

    @Inject
    lateinit var aiLifecycleObserver: AiCommentLifecycleObserver

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        RetrofitClient.init(this)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // AI WorkManager 등록
        aiWorkManager.startPeriodicWork()

        // 앱 생명주기 관찰자 등록
        ProcessLifecycleOwner.get().lifecycle.addObserver(aiLifecycleObserver)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    private fun printKeyHash() {
        try {
            val info: PackageInfo =
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )

            val signatures =
                info.signingInfo?.apkContentsSigners

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
//                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "KeyHash error", e)
        }
    }
}
