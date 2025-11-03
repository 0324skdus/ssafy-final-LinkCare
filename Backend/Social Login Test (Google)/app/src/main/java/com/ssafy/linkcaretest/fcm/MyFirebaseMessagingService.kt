package com.ssafy.linkcaretest.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.linkcaretest.MainActivity
import com.ssafy.linkcaretest.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "linkcare_notification_channel"
        private const val CHANNEL_NAME = "LinkCare 알림"
    }

    // 새로운 FCM 토큰이 생성되었을 때 호출됨
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새로운 FCM 토큰 생성: $token")

        // TODO: 여기서 토큰을 백엔드 서버로 전송해야 함
        // sendTokenToServer(token)
    }

    // 푸시 알림을 받았을 때 호출됨
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "푸시 알림 수신: ${message.from}")

        // 알림 데이터가 있는 경우
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "알림 데이터: ${message.data}")
        }

        // 알림 메시지가 있는 경우
        message.notification?.let {
            Log.d(TAG, "알림 제목: ${it.title}")
            Log.d(TAG, "알림 내용: ${it.body}")

            // 알림 표시
            showNotification(it.title, it.body)
        }
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
