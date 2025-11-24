package com.a307.linkcare.feature.watch.service

import android.content.Intent
import android.util.Log
import com.a307.linkcare.common.di.entrypoint.WorkoutServiceEntryPoint
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.watch.manager.ExerciseSessionManager
import com.a307.linkcare.feature.watch.data.mapper.toRequest
import com.a307.linkcare.feature.watch.domain.model.WorkoutSummary
import com.a307.linkcare.feature.workout.domain.repository.WorkoutRepository
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchListenerService : WearableListenerService() {

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseSessionManager: ExerciseSessionManager
    private lateinit var tokenStore: TokenStore

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPoints.get(applicationContext, WorkoutServiceEntryPoint::class.java)
        workoutRepository = entryPoint.workoutRepository()
        exerciseSessionManager = entryPoint.exerciseSessionManager()
        tokenStore = TokenStore(applicationContext)
        Log.d("WatchListenerService", "Service created, repository and sessionManager initialized")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            val path = event.dataItem.uri.path

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

            when (path) {

                "/linkcare/metrics" -> {
                    val hr = dataMap.getInt("heartRate")
                    val cal = dataMap.getInt("calories")
                    val dur = dataMap.getInt("durationSec")
                    val ts = dataMap.getLong("timestamp")

                    Log.d("DataLayer", "📡 Realtime data: HR=$hr, Cal=$cal, Dur=$dur, Ts=$ts")

                    sendBroadcast(Intent("REALTIME_METRICS_RECEIVED").apply {
                        putExtra("heartRate", hr)
                        putExtra("calories", cal)
                        putExtra("durationSec", dur)
                        putExtra("timestamp", ts)
                    })
                }

                "/linkcare/summary" -> {
                    val summary = WorkoutSummary(
                        sessionId = dataMap.getLong("sessionId"),
                        avgHeartRate = dataMap.getInt("avgHeartRate"),
                        calories = dataMap.getFloat("calories"),         // 수정
                        distance = dataMap.getFloat("distance"),         // 수정
                        durationSec = dataMap.getLong("durationSec"),    // 수정
                        startTimestamp = dataMap.getLong("startTimestamp"),
                        endTimestamp = dataMap.getLong("endTimestamp")
                    )

                    Log.d("DataLayer", "📩 Summary received: $summary")

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            workoutRepository.uploadSummary(summary.toRequest())
                            Log.d("DataLayer", "🔥 Server upload success")
                        } catch (e: Exception) {
                            Log.e("DataLayer", "❌ Server upload failed", e)
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/linkcare/session") {
            val state = String(messageEvent.data)
            Log.d("DataLayer", "📨 Session state: $state")

            // 현재 사용자의 운동 상태 업데이트
            val userId = tokenStore.getUserPk()
            if (userId != null) {
                exerciseSessionManager.updateSessionState(userId, state)
                Log.d("DataLayer", "📱 Updated exercise state for user $userId: $state")
            } else {
                Log.w("DataLayer", "⚠️ Cannot update exercise state - user not logged in")
            }

            sendBroadcast(Intent("SESSION_STATE_RECEIVED").apply {
                putExtra("state", state)
            })
        }
    }
}