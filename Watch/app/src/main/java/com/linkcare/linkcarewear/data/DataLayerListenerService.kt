package com.linkcare.linkcarewear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // 🔹 1️⃣ DataEventBuffer를 바로 복제해서 안전하게 저장
        val events = dataEvents.map { it.freeze() }  // freeze()는 DataEvent의 스냅샷을 생성

        CoroutineScope(Dispatchers.IO).launch {
            for (event in events) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val path = event.dataItem.uri.path
                    if (path == "/linkcare/theme") {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val characterId = dataMap.getLong("selected_character_id", 1001L)
                        val backgroundId = dataMap.getLong("selected_background_id", 2001L)

                        saveThemeToPrefs(applicationContext, characterId, backgroundId)
                        Log.d("DataLayer", "✅ Received theme: char=$characterId bg=$backgroundId")
                    }
                }
            }
        }
    }
    private fun saveThemeToPrefs(context: Context, characterId: Long, backgroundId: Long) {
        val prefs = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("selected_character_id", characterId)
            .putLong("selected_background_id", backgroundId)
            .apply()
    }
}
