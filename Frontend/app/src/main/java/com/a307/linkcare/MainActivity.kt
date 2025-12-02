package com.a307.linkcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.a307.linkcare.common.theme.LinkCareTheme
import com.a307.linkcare.feature.auth.data.api.ProfileApi
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.auth.data.model.request.UpdateFcmTokenRequest
import com.a307.linkcare.navigation.AppNavGraph
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var profileApi: ProfileApi

    @Inject
    lateinit var tokenStore: TokenStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("MainActivity", "알림 권한 승인됨")
        } else {
            Log.w("MainActivity", "알림 권한 거부됨 - FCM 푸시 알림을 받을 수 없습니다")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // FCM 토큰 서버 전송
        updateFcmTokenToServer()

        setContent {
            LinkCareTheme {
                // 딥링크에서 토큰 추출
                val deepLinkToken = remember {
                    intent?.data?.pathSegments?.lastOrNull()
                }

                AppNavGraph(initialDeepLinkToken = deepLinkToken)
            }
        }
    }

    /**
     * MainActivity 공개 메서드: 알림 권한 요청
     * LoginScreen에서 로그인 성공 후 호출됩니다
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MainActivity", " 알림 권한 이미 있음")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.i("MainActivity", "Android 12 이하 - 알림 권한 자동 승인")
        }
    }

    private fun updateFcmTokenToServer() {
        lifecycleScope.launch {
            try {
                // 액세스 토큰 확인
                val accessToken = tokenStore.getAccess()
                if (accessToken.isNullOrEmpty()) {
                    return@launch
                }

                // 현재 FCM 토큰 가져오기
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                // 서버에 전송
                val response = profileApi.updateFcmToken(UpdateFcmTokenRequest(fcmToken))
                if (response.isSuccessful) {
                } else {
                    response.errorBody()?.string()?.let { errorBody ->
                        Log.e("MainActivity", "에러 응답: $errorBody")
                    }
                }
            } catch (e: Exception) {
               e.printStackTrace()
            }
        }
    }
}
