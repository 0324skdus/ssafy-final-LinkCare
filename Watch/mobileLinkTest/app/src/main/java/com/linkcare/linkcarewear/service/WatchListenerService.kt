package com.linkcare.linkcarewear.service

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.linkcare.linkcarewear.model.WorkoutSummary

class WatchListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

            when (path) {
                "/linkcare/metrics" -> {
                    val hr = dataMap.getInt("heartRate")
                    val cal = dataMap.getInt("calories")
                    val dur = dataMap.getInt("durationSec")
                    val ts = dataMap.getLong("timestamp")
                    Log.d("DataLayer", "📡 Realtime data: HR=$hr, Cal=$cal, Dur=$dur,Ts=$ts")
                    sendBroadcast(Intent("REALTIME_METRICS_RECEIVED").apply {
                        putExtra("heartRate", hr)
                        putExtra("calories", cal)
                        putExtra("durationSec", dur)
                        putExtra("timestamp", ts)
                    })
                }

                "/linkcare/summary" -> {
                    val summary = WorkoutSummary(
                        sessionId = dataMap.getLong("sessionId") ?: 0L,
                        avgHeartRate = dataMap.getInt("avgHeartRate"),
                        calories = dataMap.getInt("calories"),
                        distance = dataMap.getInt("distance"),
                        durationSec = dataMap.getInt("durationSec"),
                        startTimestamp = dataMap.getLong("startTimestamp"),
                        endTimestamp = dataMap.getLong("endTimestamp")
                    )
                    Log.d("DataLayer", "📩 Summary received: $summary")
                    sendBroadcast(Intent("WORKOUT_SUMMARY_RECEIVED").apply {
                        putExtra("summary", summary.toString())
                    })
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
