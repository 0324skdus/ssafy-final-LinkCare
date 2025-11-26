package com.a307.linkcare.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import com.a307.linkcare.data.model.WorkoutSummary

object DataLayerManager {
    private const val TAG = "DataLayerManager"
    private lateinit var dataClient: DataClient
    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        dataClient = Wearable.getDataClient(appContext)
        messageClient = Wearable.getMessageClient(appContext)
        nodeClient = Wearable.getNodeClient(appContext)
    }

    /** 운동 중 실시간 데이터 전송 */
    fun sendMetrics(sessionId: Long, heartRate: Int, calories: Int, durationSec: Int) {
        val dataMap = PutDataMapRequest.create("/linkcare/metrics").apply {
            dataMap.putLong("sessionId", sessionId)
            dataMap.putInt("heartRate", heartRate)
            dataMap.putInt("calories", calories)
            dataMap.putInt("durationSec", durationSec)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }

        dataClient.putDataItem(dataMap.asPutDataRequest().setUrgent())
            .addOnSuccessListener { Log.d(TAG, "✅ metrics sent") }
            .addOnFailureListener { Log.e(TAG, "❌ metrics send failed", it) }
    }

    //세션 상태 전송(START / PAUSE / RESUME / STOP)
    fun sendSessionState(state: String) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        "/linkcare/session",
                        state.toByteArray()
                    ).addOnSuccessListener {
                        Log.d(TAG, "✅ session message sent to ${node.displayName}")
                    }.addOnFailureListener {
                        Log.e(TAG, "❌ failed to send session to ${node.displayName}", it)
                    }
                }
            }
            .addOnFailureListener { Log.e(TAG, "❌ node lookup failed", it) }
    }

    //운동 종료 후 요약 리포트 전송
    fun sendWorkoutSummary(context: Context, summary: WorkoutSummary) {
        if (summary.sessionId == 0L) {
            Log.w(TAG, "⚠️ Ignoring summary send: empty sessionId")
            return
        }
        val timestamp = System.currentTimeMillis()
        Log.d(
            TAG,
            "⚙️ sendWorkoutSummary() called at $timestamp, session=${summary.sessionId}"
        )
        val dataMap = PutDataMapRequest.create("/linkcare/summary").apply {
            dataMap.putLong("sessionId", summary.sessionId)
            dataMap.putInt("avgHeartRate", summary.avgHeartRate)
            dataMap.putFloat("calories", summary.calories)
            dataMap.putFloat("distance", summary.distance)
            dataMap.putLong("durationSec", summary.durationSec)
            dataMap.putLong("startTimestamp", summary.startTimestamp)
            dataMap.putLong("endTimestamp", summary.endTimestamp)
        }
        dataClient.putDataItem(dataMap.asPutDataRequest().setUrgent())
            .addOnSuccessListener { Log.d(TAG, "✅ summary sent") }
            .addOnFailureListener { Log.e(TAG, "❌ summary send failed", it) }
    }
}
