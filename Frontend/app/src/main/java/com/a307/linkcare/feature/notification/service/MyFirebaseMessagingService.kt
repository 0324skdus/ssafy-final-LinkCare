package com.a307.linkcare.feature.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.a307.linkcare.MainActivity
import com.a307.linkcare.R
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.auth.data.api.ProfileApi
import com.a307.linkcare.feature.auth.data.model.request.UpdateFcmTokenRequest
import com.a307.linkcare.feature.notification.manager.NotificationEventManager
import com.a307.linkcare.sdk.health.domain.repository.HealthRepository
import com.a307.linkcare.sdk.health.domain.sync.DailyHealthData
import com.a307.linkcare.sdk.health.domain.sync.activitySummary.ActivitySummaryReader
import com.a307.linkcare.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.a307.linkcare.sdk.health.domain.sync.exercise.ExerciseReader
import com.a307.linkcare.sdk.health.domain.sync.heartRate.HeartRateReader
import com.a307.linkcare.sdk.health.domain.sync.sleep.SleepReader
import com.a307.linkcare.sdk.health.domain.sync.step.StepReader
import com.a307.linkcare.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MyFirebaseMessagingServiceEntryPoint {
        fun healthRepository(): HealthRepository
        fun tokenStore(): TokenStore
        fun heartRateReader(): HeartRateReader
        fun sleepReader(): SleepReader
        fun bloodPressureReader(): BloodPressureReader
        fun waterIntakeReader(): WaterIntakeReader
        fun exerciseReader(): ExerciseReader
        fun stepReader(): StepReader
        fun activitySummaryReader(): ActivitySummaryReader
        fun notificationEventManager(): NotificationEventManager
    }

    private lateinit var notificationEventManager: NotificationEventManager

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

    private lateinit var healthRepository: HealthRepository
    private lateinit var tokenStore: TokenStore
    private lateinit var heartRateReader: HeartRateReader
    private lateinit var sleepReader: SleepReader
    private lateinit var bloodPressureReader: BloodPressureReader
    private lateinit var waterIntakeReader: WaterIntakeReader
    private lateinit var exerciseReader: ExerciseReader
    private lateinit var stepReader: StepReader
    private lateinit var activitySummaryReader: ActivitySummaryReader

    override fun onCreate() {
        super.onCreate()
        initializeDependencies()
    }

    private fun initializeDependencies() {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MyFirebaseMessagingServiceEntryPoint::class.java
        )

        healthRepository = entryPoint.healthRepository()
        tokenStore = entryPoint.tokenStore()
        heartRateReader = entryPoint.heartRateReader()
        sleepReader = entryPoint.sleepReader()
        bloodPressureReader = entryPoint.bloodPressureReader()
        waterIntakeReader = entryPoint.waterIntakeReader()
        exerciseReader = entryPoint.exerciseReader()
        stepReader = entryPoint.stepReader()
        activitySummaryReader = entryPoint.activitySummaryReader()
        notificationEventManager = entryPoint.notificationEventManager()
    }

    // 새로운 FCM 토큰이 생성되었을 때 호출됨
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // 백엔드 서버로 토큰 전송
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                val tokenStore = TokenStore(applicationContext)
                val accessToken = tokenStore.getAccess()

                if (accessToken.isNullOrEmpty()) {
                    return@launch
                }

                // Retrofit 인스턴스 생성
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
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
                val response = api.updateFcmToken(UpdateFcmTokenRequest(token))

                if (response.isSuccessful) {
                    Log.i(TAG, "FCM 토큰 서버 전송 성공")
                } else {
                   response.errorBody()?.string()?.let { errorBody ->
                        Log.e(TAG, "에러 응답: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM 토큰 서버 전송 중 에러: ${e.message}", e)
            }
        }
    }

    // 푸시 알림을 받았을 때 호출됨
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notificationType = message.data["type"]
        val groupType = message.data["groupType"]
        val title = message.data["title"] ?: "LinkCare"
        val body  = message.data["body"] ?: "새로운 알림이 있습니다"

        // 타입에 따른 처리
        if (!message.data.isNullOrEmpty()) {
            if (notificationType == "GROUP_JOIN_APPROVED") {
                sendGroupJoinedBroadcast(groupType)
            }

            when (notificationType) {
                "DAILY_SYNC" -> handleDailySyncRequest()
                "EXERCISE_SYNC" -> handleExerciseSyncRequest()
                else -> Log.d(TAG, "알 수 없는 타입: $notificationType")
            }
        }

        // 항상 우리가 직접 알림 + 브로드캐스트
        showNotification(title, body)
        sendNewNotificationBroadcast()
    }

    private fun sendNewNotificationBroadcast() {
        // SharedFlow를 사용한 이벤트 전송
        try {
            if (::notificationEventManager.isInitialized) {
                serviceScope.launch {
                    notificationEventManager.notifyNewNotification()
                }
            } else {
                Log.w(TAG, "NotificationEventManager가 초기화되지 않음")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SharedFlow 이벤트 전송 실패: ${e.message}", e)
        }

        // 기존 브로드캐스트도 유지
        val intent = Intent(ACTION_NEW_NOTIFICATION).apply {
            setPackage(packageName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        sendBroadcast(intent)
    }

    private fun sendGroupJoinedBroadcast(groupType: String?) {
        // SharedFlow를 사용한 이벤트 전송
        try {
            if (::notificationEventManager.isInitialized) {
                serviceScope.launch {
                    notificationEventManager.notifyNewNotification()
                }
            } else {
                Log.w(TAG, "NotificationEventManager가 초기화되지 않음")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SharedFlow 이벤트 전송 실패: ${e.message}", e)
        }

        // 기존 브로드캐스트도 유지
        val intent = Intent(ACTION_GROUP_JOINED).apply {
            putExtra(EXTRA_GROUP_TYPE, groupType ?: "")
            setPackage(packageName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        sendBroadcast(intent)
    }

    /**
     * 일일 건강 데이터 동기화 처리
     */
    private fun handleDailySyncRequest() {
        serviceScope.launch {
            try {
                val userId = tokenStore.getUserPk()?.toInt()
                if (userId == null) {
                    return@launch
                }

                val healthData = readDailyHealthData()

                // 백엔드로 전송
                val result = healthRepository.uploadUserDailyHealthData(userId, healthData)

                if (result.isSuccess) {
                    Log.d(TAG, "건강 데이터 동기화 성공")
                } else {
                    Log.e(TAG, "건강 데이터 동기화 실패: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "건강 데이터 동기화 중 오류 발생", e)
            }
        }
    }

    /**
     * 운동 데이터 동기화 처리
     */
    private fun handleExerciseSyncRequest() {
        serviceScope.launch {
            try {
                val userId = tokenStore.getUserPk()?.toInt()
                if (userId == null) {
                    Log.w(TAG, "사용자 정보를 찾을 수 없습니다")
                    return@launch
                }

                // 운동 데이터만 읽기
                val exerciseData = try {
                    exerciseReader.readToday()
                } catch (e: Exception) {
                    Log.e(TAG, "운동 데이터 읽기 실패", e)
                    null
                }

                exerciseData?.let {
                    Log.d(TAG, "운동 데이터 동기화 완료")
                }
            } catch (e: Exception) {
                Log.e(TAG, "운동 데이터 동기화 중 오류 발생", e)
            }
        }
    }

    /**
     * 삼성헬스에서 하루치 데이터 읽기
     */
    private suspend fun readDailyHealthData(): DailyHealthData {
        return DailyHealthData(
            activitySummary = try {
                activitySummaryReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "활동 요약 읽기 실패", e)
                null
            },
            heartRate = try {
                heartRateReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "심박수 읽기 실패", e)
                null
            },
            sleep = try {
                sleepReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "수면 읽기 실패", e)
                null
            },
            waterIntake = try {
                waterIntakeReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "음수량 읽기 실패", e)
                null
            },
            bloodPressure = try {
                bloodPressureReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "혈압 읽기 실패", e)
                null
            },
            exercise = try {
                exerciseReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "운동 읽기 실패", e)
                null
            },
            step = try {
                stepReader.readToday()
            } catch (e: Exception) {
                Log.e(TAG, "걸음수 읽기 실패", e)
                null
            }
        )
    }

    private fun showNotification(title: String?, body: String?) {
        // 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannel()

        // 알림 클릭 시 실행될 인텐트
        val intent = Intent(this, MainActivity::class.java).apply {
            Intent.setFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    override fun onDestroy() {
        super.onDestroy()
        // 코루틴 스코프 정리
        serviceScope.cancel()
    }
}