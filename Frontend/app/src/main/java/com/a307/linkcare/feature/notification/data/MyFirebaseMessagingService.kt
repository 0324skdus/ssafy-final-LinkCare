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
import com.a307.linkcare.sdk.health.data.repository.HealthRepository
import com.a307.linkcare.sdk.health.domain.sync.*
import com.a307.linkcare.sdk.health.domain.sync.activitySummary.ActivitySummaryReader
import com.a307.linkcare.sdk.health.domain.sync.bloodPressure.BloodPressureReader
import com.a307.linkcare.sdk.health.domain.sync.exercise.ExerciseReader
import com.a307.linkcare.sdk.health.domain.sync.heartRate.HeartRateReader
import com.a307.linkcare.sdk.health.domain.sync.sleep.SleepReader
import com.a307.linkcare.sdk.health.domain.sync.step.StepReader
import com.a307.linkcare.sdk.health.domain.sync.waterIntake.WaterIntakeReader
import com.a307.linkcare.feature.auth.data.ProfileApi
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.auth.domain.UpdateFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*

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
    }

    @Inject
    lateinit var notificationEventManager: NotificationEventManager

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
    }

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
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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

            // 데이터 타입 확인
            when (message.data["type"]) {
                "DAILY_SYNC" -> {
                    Log.d(TAG, "일일 건강 데이터 동기화 요청 수신")
                    handleDailySyncRequest()
                }
                "EXERCISE_SYNC" -> {
                    Log.d(TAG, "운동 데이터 동기화 요청 수신")
                    handleExerciseSyncRequest()
                }
                else -> {
                    Log.d(TAG, "알 수 없는 타입: ${message.data["type"]}")
                }
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
        // SharedFlow를 사용한 이벤트 전송
        serviceScope.launch {
            notificationEventManager.notifyNewNotification()
            Log.d(TAG, "새 알림 이벤트 전송 (SharedFlow)")
        }

        // 기존 브로드캐스트도 유지 (호환성)
        val intent = Intent(ACTION_NEW_NOTIFICATION).apply {
            setPackage(packageName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        sendBroadcast(intent)
        Log.d(TAG, "새 알림 브로드캐스트 전송 (Backup)")
    }

    private fun sendGroupJoinedBroadcast(groupType: String?) {
        // SharedFlow를 사용한 이벤트 전송
        serviceScope.launch {
            notificationEventManager.notifyNewNotification()
            Log.d(TAG, "그룹 가입 이벤트 전송 (SharedFlow): $groupType")
        }

        // 기존 브로드캐스트도 유지 (호환성)
        val intent = Intent(ACTION_GROUP_JOINED).apply {
            putExtra(EXTRA_GROUP_TYPE, groupType ?: "")
            setPackage(packageName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        sendBroadcast(intent)
        Log.d(TAG, "그룹 가입 브로드캐스트 전송 (Backup): $groupType")
    }

    /**
     * 일일 건강 데이터 동기화 처리
     */
    private fun handleDailySyncRequest() {
        serviceScope.launch {
            try {
                val userId = tokenStore.getUserPk()?.toInt()
                if (userId == null) {
                    Log.w(TAG, "사용자 정보를 찾을 수 없습니다")
                    return@launch
                }

                Log.d(TAG, "사용자 $userId 건강 데이터 동기화 시작")
                val healthData = readDailyHealthData()

                // 읽은 데이터 로그
                Log.d(TAG, "=== FCM: 읽은 건강 데이터 ===")
                Log.d(TAG, "ActivitySummary: ${healthData.activitySummary}")
                Log.d(TAG, "HeartRate: ${healthData.heartRate?.size} items")
                Log.d(TAG, "Sleep: ${healthData.sleep?.size} items")
                Log.d(TAG, "WaterIntake: ${healthData.waterIntake}")
                Log.d(TAG, "BloodPressure: ${healthData.bloodPressure?.size} items")
                Log.d(TAG, "Exercise: ${healthData.exercise}")
                Log.d(TAG, "Step: ${healthData.step}")
                Log.d(TAG, "=============================")

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

                Log.d(TAG, "사용자 $userId 운동 데이터 동기화 시작")

                // 운동 데이터만 읽기
                val exerciseData = try {
                    exerciseReader.readToday()
                } catch (e: Exception) {
                    Log.e(TAG, "운동 데이터 읽기 실패", e)
                    null
                }

                exerciseData?.let {
                    // TODO: 운동 데이터만 업로드하는 API 호출
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
        Log.d(TAG, "건강 데이터 읽기 시작")

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

    override fun onDestroy() {
        super.onDestroy()
        // 코루틴 스코프 정리
        serviceScope.cancel()
    }
}
