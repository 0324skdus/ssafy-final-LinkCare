package com.a307.linkcare.sdk.health.domain.sync.common

import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.device.Device
import java.time.ZoneId

/**
 * 여러 API 호출이 필요한 경우 사용
 * (음수량: 데이터 + 총량 + 목표)
 * (운동: 데이터 + 총 시간 + 총 칼로리)
 */
abstract class CompositeHealthReader<T>(
    private val deviceManager: DeviceManager
) {
    protected val KST: ZoneId = ZoneId.of("Asia/Seoul")

    /**
     * 오늘 하루 데이터 조회
     * 여러 API를 조합하여 반환
     */
    abstract suspend fun readToday(): T

    /**
     * 전체 기간 데이터 조회
     * @param onProgress 진행 상황 콜백
     */
    abstract suspend fun readAll(
        yearsPerChunk: Long = 1L,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): List<T>

    suspend fun findDevice(
        deviceManager: DeviceManager
    ): Device {
        val device = deviceManager.getLocalDevice()
        return device
    }
}