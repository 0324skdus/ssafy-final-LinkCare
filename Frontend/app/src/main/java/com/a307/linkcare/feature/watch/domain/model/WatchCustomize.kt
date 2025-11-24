package com.a307.linkcare.feature.watch.domain.model

data class WatchCustomize(
    val characterId: Int,
    val backgroundId: Int
) {
    companion object {
        fun default() = WatchCustomize(1, 1)
    }
}