package com.a307.linkcare.sdk.health.domain.sync.common

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.device.Device
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 단순 readData 요청만 필요한 경우 사용
 * (심박수, 수면, 혈압)
 */
abstract class SimpleHealthReader<T>(
    protected val store: HealthDataStore,
    private val deviceManager: DeviceManager,
    ) {
    protected val KST: ZoneId = ZoneId.of("Asia/Seoul")

    protected abstract suspend fun buildTodayRequest(): ReadDataRequest<HealthDataPoint>
    protected abstract suspend fun buildRangeRequest(start: Instant, end: Instant): ReadDataRequest<HealthDataPoint>
    protected abstract suspend fun mapToData(dataPoint: HealthDataPoint, deviceManager: DeviceManager): T

    /**
     * 오늘 하루 데이터 조회 (매일 동기화)
     */
    suspend fun readToday(): List<T> = withContext(Dispatchers.IO) {
        val request = buildTodayRequest()
        val result = store.readData(request)
        result.dataList.map { mapToData(it, deviceManager) }
    }

    /**
     * 전체 데이터 동기화 (최초 1회)
     * @param yearsPerChunk 한 번에 조회할 연도 수 (기본 1년)
     * @param onProgress 진행 상황 콜백
     * @param enableDelay API 호출 제한 대응을 위한 딜레이 활성화 (기본 true)
     */
    suspend fun readAll(
        yearsPerChunk: Long = 1L,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
        enableDelay: Boolean = true
    ): List<T> = withContext(Dispatchers.IO) {
        val windows = generateYearWindows(yearsPerChunk)
        val result = ArrayList<T>(2048)

        windows.forEachIndexed { index, (start, end) ->
            val request = buildRangeRequest(start, end)
            val response = store.readData(request)
            response.dataList.forEach { result += mapToData(it, deviceManager) }

            onProgress?.invoke(index + 1, windows.size)

            // API 호출 제한 대응: 각 요청 사이에 딜레이 추가
            if (enableDelay && index < windows.size - 1) {
                delay(100) // 100ms 대기
            }
        }

        result
    }

    suspend fun findDevice(
        deviceManager: DeviceManager
    ): Device {
        val device = deviceManager.getLocalDevice()
        return device
    }

    private fun generateYearWindows(yearsPerChunk: Long): List<Pair<Instant, Instant>> {
        val now = Instant.now()
        var cursor = Instant.now().minus(Duration.ofDays(20 * 365L))
        val windows = mutableListOf<Pair<Instant, Instant>>()

        while (cursor < now) {
            val next = cursor.atZone(ZoneOffset.UTC)
                .plusYears(yearsPerChunk)
                .toInstant()
                .let { if (it > now) now else it }
            windows.add(cursor to next)
            cursor = next
        }

        return windows
    }
}