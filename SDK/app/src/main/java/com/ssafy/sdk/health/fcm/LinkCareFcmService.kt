package com.ssafy.sdk.health.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.sdk.health.domain.upload.SyncAndUploadHealthDataUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LinkCareFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var syncAndUploadHealthDataUseCase: SyncAndUploadHealthDataUseCase

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // 메시지 데이터 확인
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")

            val syncType = remoteMessage.data["type"]
            val userId = remoteMessage.data["userId"]?.toIntOrNull()

            when (syncType) {
                "health_sync_daily" -> handleDailySync(userId)
                "health_sync_all" -> handleFullSync(userId)
                "health_sync_exercise" -> handleExerciseOnlySync(userId)
                else -> Log.d("FCM", "Unknown sync type: $syncType")
            }
        }

        // 알림 메시지 처리
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
        }
    }

    /**
     * 하루치 데이터 동기화 & 업로드
     */
    private fun handleDailySync(userId: Int?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FCM", "Starting daily health data sync for user: $userId")

                syncAndUploadHealthDataUseCase.syncAndUploadDaily(userId)
                    .onSuccess {
                        Log.d("FCM", "Daily health data sync completed successfully")
                    }
                    .onFailure { e ->
                        Log.e("FCM", "Daily health data sync failed", e)
                    }
            } catch (e: Exception) {
                Log.e("FCM", "Error during daily health sync", e)
            }
        }
    }

    /**
     * 전체 데이터 동기화 & 업로드
     */
    private fun handleFullSync(userId: Int?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FCM", "Starting full health data sync for user: $userId")

                syncAndUploadHealthDataUseCase.syncAndUploadAll(
                    userId = userId,
                    onProgress = { dataType, current, total ->
                        Log.d("FCM", "Syncing $dataType: $current/$total")
                    }
                ).onSuccess {
                    Log.d("FCM", "Full health data sync completed successfully")
                }.onFailure { e ->
                    Log.e("FCM", "Full health data sync failed", e)
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error during full health sync", e)
            }
        }
    }

    /**
     * 운동 데이터만 동기화 & 업로드
     */
    private fun handleExerciseOnlySync(userId: Int?) {
        if (userId == null) {
            Log.e("FCM", "userId is required for exercise sync")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FCM", "Starting exercise data sync for user: $userId")

                syncAndUploadHealthDataUseCase.syncAndUploadExerciseOnly(userId)
                    .onSuccess {
                        Log.d("FCM", "Exercise data sync completed successfully")
                    }
                    .onFailure { e ->
                        Log.e("FCM", "Exercise data sync failed", e)
                    }
            } catch (e: Exception) {
                Log.e("FCM", "Error during exercise sync", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token: $token")

        // TODO: 필요시 서버에 새 토큰 전송
        // sendTokenToServer(token)
    }
}