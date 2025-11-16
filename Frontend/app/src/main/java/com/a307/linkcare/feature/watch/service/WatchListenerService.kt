package com.a307.linkcare.feature.watch.service

import android.content.Intent
import android.util.Log
import com.a307.linkcare.feature.watch.data.mapper.toRequest
import com.a307.linkcare.feature.watch.model.WorkoutSummary
import com.a307.linkcare.feature.workout.data.repository.WorkoutRepository
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint  // ❗ 이것도 필요하면 추가
class WatchListenerService : WearableListenerService() {

    @Inject lateinit var workoutRepository: WorkoutRepository

    override fun onCreate() {
        super.onCreate()
        EntryPoints.get(applicationContext, WorkoutServiceEntryPoint::class.java)
            .inject(this)
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
            sendBroadcast(Intent("SESSION_STATE_RECEIVED").apply {
                putExtra("state", state)
            })
        }
    }
}