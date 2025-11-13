package com.linkcare.linkcarewear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object DataLayerManager {

    private const val PATH_THEME = "/linkcare/theme"

    fun sendThemeToWatch(
        context: Context,
        characterId: Long,
        backgroundId: Long
    ) {
        val dataClient = Wearable.getDataClient(context)

        val dataMap = PutDataMapRequest.create(PATH_THEME).apply {
            dataMap.putLong("selected_character_id", characterId)
            dataMap.putLong("selected_background_id", backgroundId)
            dataMap.putLong("timestamp", System.currentTimeMillis()) // 변경감지 및 중복 방지용
        }

        val request = dataMap.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnSuccessListener {
                Log.d("DataLayer", "✅ Sent theme to watch: $characterId / $backgroundId")
            }
            .addOnFailureListener {
                Log.e("DataLayer", "❌ Failed to send theme to watch", it)
            }
    }
}
