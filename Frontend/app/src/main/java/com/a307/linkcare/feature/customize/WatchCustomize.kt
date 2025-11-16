package com.a307.linkcare.feature.customize

data class WatchCustomize(
    val characterId: Int,
    val backgroundId: Int
) {
    companion object {
        fun default() = WatchCustomize(1, 1)
    }
}