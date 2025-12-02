package com.a307.linkcare.common.network.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "processed_notifications")

class ProcessedNotificationStore(private val context: Context) {

    companion object {
        private val PROCESSED_NOTIFICATIONS_KEY = stringPreferencesKey("processed_notifications")
    }

    // 처리된 알림 정보를 Map<Long, String> 형태로 가져오기
    val processedNotifications: Flow<Map<Long, String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[PROCESSED_NOTIFICATIONS_KEY] ?: "{}"
        parseJsonToMap(jsonString)
    }

    // 처리된 알림 추가
    suspend fun addProcessedNotification(notificationId: Long, message: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PROCESSED_NOTIFICATIONS_KEY] ?: "{}"
            val currentMap = parseJsonToMap(current).toMutableMap()
            currentMap[notificationId] = message
            preferences[PROCESSED_NOTIFICATIONS_KEY] = mapToJson(currentMap)
        }
    }

    // 처리된 알림 제거
    suspend fun removeProcessedNotification(notificationId: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[PROCESSED_NOTIFICATIONS_KEY] ?: "{}"
            val currentMap = parseJsonToMap(current).toMutableMap()
            currentMap.remove(notificationId)
            preferences[PROCESSED_NOTIFICATIONS_KEY] = mapToJson(currentMap)
        }
    }

    // 간단한 JSON 파싱 (Gson 대신 수동 파싱)
    private fun parseJsonToMap(json: String): Map<Long, String> {
        if (json == "{}") return emptyMap()

        return try {
            json.trim('{', '}')
                .split(",")
                .filter { it.isNotBlank() }
                .associate { entry ->
                    val (key, value) = entry.split(":")
                    key.trim().toLong() to value.trim('"')
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Map을 JSON 문자열로 변환
    private fun mapToJson(map: Map<Long, String>): String {
        if (map.isEmpty()) return "{}"

        return map.entries.joinToString(",", "{", "}") { (key, value) ->
            "$key:\"$value\""
        }
    }
}
