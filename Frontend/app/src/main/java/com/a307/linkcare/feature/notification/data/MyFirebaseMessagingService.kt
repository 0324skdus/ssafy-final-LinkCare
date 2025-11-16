package com.a307.linkcare.feature.notification.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.a307.linkcare.MainActivity
import com.a307.linkcare.R
import com.a307.linkcare.feature.auth.data.ProfileApi
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.auth.domain.UpdateFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "linkcare_notification_channel"
        private const val CHANNEL_NAME = "LinkCare 알림"
        const val ACTION_GROUP_JOINED = "com.a307.linkcare.GROUP_JOINED"
        const val ACTION_NEW_NOTIFICATION = "com.a307.linkcare.NEW_NOTIFICATION"
        const val EXTRA_GROUP_TYPE = "group_type"
        private const val BASE_URL = "http://k13a307.p.ssafy.io:9090/"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // 새로운 FCM 토큰이 생성되었을 때 호출됨
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "=== 새로운 FCM 토큰 생성 ===")
        Log.i(TAG, "토큰: $token")

        // 백엔드 서버로 토큰 전송
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "FCM 토큰 서버 전송 시작")
                val tokenStore = TokenStore(applicationContext)
                val accessToken = tokenStore.getAccess()

                if (accessToken.isNullOrEmpty()) {
                    Log.w(TAG, "액세스 토큰이 없어 FCM 토큰 전송 불가")
                    return@launch
                }

                Log.d(TAG, "액세스 토큰 확인 완료, API 호출 준비")

                // Retrofit 인스턴스 생성
                val client = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $accessToken")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ProfileApi::class.java)
                Log.d(TAG, "API 호출 중: updateFcmToken")
                val response = api.updateFcmToken(UpdateFcmTokenRequest(token))

                if (response.isSuccessful) {
                    Log.i(TAG, "✅ FCM 토큰 서버 전송 성공!")
                } else {
                    Log.e(TAG, "❌ FCM 토큰 서버 전송 실패: ${response.code()} - ${response.message()}")
                    response.errorBody()?.string()?.let { errorBody ->
                        Log.e(TAG, "에러 응답: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FCM 토큰 서버 전송 중 에러: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    // 푸시 알림을 받았을 때 호출됨
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "푸시 알림 수신: ${message.from}")

        // 알림 데이터가 있는 경우
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "알림 데이터: ${message.data}")

            // 알림 타입 확인
            val notificationType = message.data["type"]
            val groupType = message.data["groupType"]

            // GROUP_JOIN_APPROVED 알림인 경우 자동 새로고침
            if (notificationType == "GROUP_JOIN_APPROVED") {
                Log.d(TAG, "그룹 가입 승인 알림 수신, 자동 새로고침 트리거")
                sendGroupJoinedBroadcast(groupType)
            }
        }

        // 알림 메시지가 있는 경우 (포그라운드/백그라운드 모두 알림 표시)
        message.notification?.let {
            Log.d(TAG, "알림 제목: ${it.title}")
            Log.d(TAG, "알림 내용: ${it.body}")

            // 앱이 포그라운드든 백그라운드든 항상 알림 표시
            showNotification(it.title, it.body)

            // 새 알림 브로드캐스트 전송 (UI 실시간 업데이트용)
            sendNewNotificationBroadcast()
        }
    }

    private fun sendNewNotificationBroadcast() {
        val intent = Intent(ACTION_NEW_NOTIFICATION)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "새 알림 브로드캐스트 전송")
    }

    private fun sendGroupJoinedBroadcast(groupType: String?) {
        val intent = Intent(ACTION_GROUP_JOINED).apply {
            putExtra(EXTRA_GROUP_TYPE, groupType ?: "")
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "그룹 가입 브로드캐스트 전송: $groupType")
    }

    private fun showNotification(title: String?, body: String?) {
        // 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannel()

        // 알림 클릭 시 실행될 인텐트
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌더
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
            .setContentTitle(title ?: "LinkCare")
            .setContentText(body ?: "새로운 알림이 있습니다")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 알림 클릭 시 자동으로 삭제

        // 알림 표시
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        // Android 8.0 (API 26) 이상에서만 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "LinkCare 앱의 알림을 받기 위한 채널입니다"
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
