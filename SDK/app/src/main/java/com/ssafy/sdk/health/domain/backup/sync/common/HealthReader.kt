package com.ssafy.sdk.health.domain.backup.sync.common

interface HealthReader<T> {
    suspend fun readToday(): List<T>
    suspend fun readAll(
        windowing: Windowing = Windowing.Single,
        onProgress: ((done: Int, total: Int) -> Unit)? = null
    ): List<T>
}