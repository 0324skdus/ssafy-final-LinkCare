package com.a307.linkcare.common.network.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
//로컬 설정 저장소
class CustomizeLocalStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_CHARACTER = intPreferencesKey("custom_character_id")
        private val KEY_BACKGROUND = intPreferencesKey("custom_background_id")
    }

    val customFlow: Flow<CustomEntity> = dataStore.data.map { prefs ->
        CustomEntity(
            characterId = prefs[KEY_CHARACTER] ?: 1,
            backgroundId = prefs[KEY_BACKGROUND] ?: 1
        )
    }

    suspend fun saveCustom(characterId: Int, backgroundId: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CHARACTER] = characterId
            prefs[KEY_BACKGROUND] = backgroundId
        }
    }

    fun loadCustom(): Pair<Int, Int> {
        return 1 to 1
    }
}

data class CustomEntity(
    val characterId: Int,
    val backgroundId: Int
)