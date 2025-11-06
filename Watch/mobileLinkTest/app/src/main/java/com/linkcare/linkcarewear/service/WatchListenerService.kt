package com.linkcare.linkcarewear.service

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.linkcare.linkcarewear.model.WorkoutSummary

class WatchListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/linkcare/summary"
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val summary = WorkoutSummary(
                    sessionId = dataMap.getString("sessionId") ?: "",
                    avgHeartRate = dataMap.getInt("avgHeartRate"),
                    calories = dataMap.getInt("calories"),
                    distance = dataMap.getInt("distance"),
                    durationSec = dataMap.getInt("durationSec")
                )

                Log.d("DataLayer", "📩 Received summary: $summary")

                // 브로드캐스트로 Activity에 전달
                val intent = Intent("WORKOUT_SUMMARY_RECEIVED")
                intent.putExtra("summary", summary.toString())
                sendBroadcast(intent)
            }
        }
    }
}
