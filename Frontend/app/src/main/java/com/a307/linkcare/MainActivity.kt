package com.a307.linkcare

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.a307.linkcare.common.theme.LinkCareTheme
import com.a307.linkcare.feature.auth.data.ProfileApi
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.auth.domain.UpdateFcmTokenRequest
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

    private fun updateFcmTokenToServer() {
        lifecycleScope.launch {
            try {
                // 액세스 토큰 확인
                val accessToken = tokenStore.getAccess()
                if (accessToken.isNullOrEmpty()) {
                    Log.d("MainActivity", "액세스 토큰이 없어 FCM 토큰 전송 스킵")
                    return@launch
                }

                // 현재 FCM 토큰 가져오기
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                Log.d("MainActivity", "현재 FCM 토큰: $fcmToken")

                // 서버에 전송
                val response = profileApi.updateFcmToken(UpdateFcmTokenRequest(fcmToken))
                if (response.isSuccessful) {
                    Log.i("MainActivity", "FCM 토큰 서버 전송 성공")
                } else {
                    Log.e("MainActivity", "FCM 토큰 서버 전송 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "FCM 토큰 처리 중 에러: ${e.message}", e)
            }
        }
    }
}
