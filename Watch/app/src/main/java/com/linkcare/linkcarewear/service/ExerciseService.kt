package com.linkcare.linkcarewear.service

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.health.services.client.data.ExerciseState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.linkcare.linkcarewear.data.DataLayerManager
import com.linkcare.linkcarewear.data.ExerciseClientManager
import com.linkcare.linkcarewear.data.isExerciseInProgress
import com.linkcare.linkcarewear.data.model.WorkoutSummary
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseService : LifecycleService() {

    @Inject lateinit var exerciseClientManager: ExerciseClientManager
    @Inject lateinit var exerciseNotificationManager: ExerciseNotificationManager
    @Inject lateinit var exerciseServiceMonitor: ExerciseServiceMonitor

    private var isBound = false
    private var isStarted = false
    private val localBinder = LocalBinder()

    //중복 방지 플래그
    private var summarySent = false
    private var isExerciseEnded = false
    private var currentSessionId: String = ""

    // companion으로 세션 단위 중복 방지
    companion object {
        private val UNBIND_DELAY = 3.seconds
        private val sentSessionSummaries = mutableSetOf<String>()
    }

    val sessionId: String?
        get() = currentSessionId

    private val serviceRunningInForeground: Boolean
        get() = this.foregroundServiceType != 0

    private suspend fun isExerciseInProgress() =
        exerciseClientManager.exerciseClient.isExerciseInProgress()

    suspend fun prepareExercise() {
        exerciseClientManager.prepareExercise()
    }

    suspend fun startExercise(sessionId: String) {
        currentSessionId = sessionId
        summarySent = false
        isExerciseEnded = false
        postOngoingActivityNotification()
        Log.d("SessionDebug", "🚀 Service startExercise() currentSessionId=$currentSessionId")
        exerciseClientManager.startExercise()
    }

    suspend fun pauseExercise() {
        exerciseClientManager.pauseExercise()
    }

    suspend fun resumeExercise() {
        exerciseClientManager.resumeExercise()
    }
    suspend fun endExercise() {
        Log.d(TAG, "🟡 endExercise() CALLED, sessionId=$currentSessionId")

        if (isExerciseEnded || sentSessionSummaries.contains(currentSessionId)) return
        isExerciseEnded = true

        // ⚙️ 1️⃣ 우선 세션 종료 요청
        exerciseClientManager.endExercise()

        // ⚙️ 2️⃣ 300~500ms 기다려서 마지막 업데이트 반영
        delay(500)

        // ⚙️ 3️⃣ 이제 상태 가져오기
        val currentState = exerciseServiceMonitor.exerciseServiceState.value
        val metrics = currentState.exerciseMetrics
        val durationSec = (currentState.activeDurationCheckpoint?.activeDuration?.seconds ?: 0L).toInt()

        val summary = WorkoutSummary(
            sessionId = currentSessionId,
            avgHeartRate = (metrics.heartRateAverage ?: 0.0).toInt(),
            calories = (metrics.calories ?: 0.0).toInt(),
            distance = (metrics.distance ?: 0.0).toInt(),
            durationSec = durationSec
        )

        DataLayerManager.sendWorkoutSummary(this, summary)
        sentSessionSummaries.add(currentSessionId)
        Log.d(TAG, "✅ Workout summary sent after delay: $summary")

        removeOngoingActivityNotification()
        currentSessionId = ""
    }

    fun markLap() {
        lifecycleScope.launch {
            exerciseClientManager.markLap()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand")

        if (!isStarted) {
            isStarted = true
            if (!isBound) stopSelfIfNotRunning()

            lifecycleScope.launch(Dispatchers.Default) {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    exerciseServiceMonitor.monitor()
                }
            }
        }
        return START_STICKY
    }

    private fun stopSelfIfNotRunning() {
        lifecycleScope.launch {
            if (!isExerciseInProgress() && !isExerciseEnded) {
                // 이미 종료된 세션은 다시 endExercise() 안 부르게 조건 강화
                if (exerciseServiceMonitor.exerciseServiceState.value.exerciseState ==
                    ExerciseState.PREPARING
                ) {
                    lifecycleScope.launch {
                        endExercise()
                    }
                }
                // We have nothing to do, so we can stop.
                stopSelf()
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        DataLayerManager.init(this)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        handleBind()

        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        handleBind()
    }

    private fun handleBind() {
        if (!isBound) {
            isBound = true
            // Start ourself. This will begin collecting exercise state if we aren't already.
            startService(Intent(this, this::class.java))
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        lifecycleScope.launch {
            delay(UNBIND_DELAY)
            if (!isBound) {
                stopSelfIfNotRunning()
            }
        }
        // Allow clients to re-bind. We will be informed of this in onRebind().
        return true
    }

    fun removeOngoingActivityNotification() {
        if (serviceRunningInForeground) {
            Log.d(TAG, "Removing ongoing activity notification")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun postOngoingActivityNotification() {
        if (!serviceRunningInForeground) {
            Log.d(TAG, "Posting ongoing activity notification")

            exerciseNotificationManager.createNotificationChannel()
            val serviceState = exerciseServiceMonitor.exerciseServiceState.value
            ServiceCompat.startForeground(
                this,
                ExerciseNotificationManager.NOTIFICATION_ID,
                exerciseNotificationManager.buildNotification(
                    serviceState.activeDurationCheckpoint?.activeDuration
                        ?: Duration.ZERO
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    if
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    } else {
                        0
                    }
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@ExerciseService
        val exerciseServiceState: Flow<ExerciseServiceState>
            get() = this@ExerciseService.exerciseServiceMonitor.exerciseServiceState
    }
}
