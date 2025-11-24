package com.a307.linkcare.feature.watch.domain.respository

import com.a307.linkcare.common.network.store.CustomizeLocalStore
import javax.inject.Inject

class CustomizeRepository @Inject constructor(
    private val store: CustomizeLocalStore
) {
    val customFlow = store.customFlow

    suspend fun saveCustom(characterId: Int, backgroundId: Int) {
        store.saveCustom(characterId, backgroundId)
    }

    fun loadCustom(): Pair<Int, Int> {
        return store.loadCustom()
    }
}