package com.a307.linkcare.data

import android.content.Context
import android.util.Log
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

                        // 🔹 모바일과 동일한 key로 맞춤
                        val charId = dataMap.getInt("characterId", 1)
                        val bgId = dataMap.getInt("backgroundId", 1)

                        saveThemeToPrefs(applicationContext, charId, bgId)

                        Log.d("DataLayer", "⬅️ Received theme from phone: char=$charId bg=$bgId")
                    }
                }
            }
        }
    }

    private fun saveThemeToPrefs(context: Context, characterId: Int, backgroundId: Int) {
        val prefs = context.getSharedPreferences("customize_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("characterId", characterId)
            .putInt("backgroundId", backgroundId)
            .apply()
    }
}
