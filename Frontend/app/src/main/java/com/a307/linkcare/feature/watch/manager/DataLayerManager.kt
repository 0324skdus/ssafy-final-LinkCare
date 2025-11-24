package com.a307.linkcare.feature.watch.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object DataLayerManager {

    private const val PATH_THEME = "/linkcare/theme"

    fun sendTheme(
        context: Context,
        characterId: Int,
        backgroundId: Int
    ) {
        val appContext = context.applicationContext
        val dataClient = Wearable.getDataClient(appContext)

        val dataMap = PutDataMapRequest.create(PATH_THEME).apply {
            dataMap.putInt("characterId", characterId)
            dataMap.putInt("backgroundId", backgroundId)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }

        val request = dataMap.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnSuccessListener {
                Log.d("DataLayer", "Sent theme to watch: $characterId / $backgroundId")
            }
            .addOnFailureListener {
                Log.e("DataLayer", "Failed to send theme to watch", it)
            }
    }
}
