package com.ssafy.sdk.health.domain.sync

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.ssafy.sdk.health.data.test.HeartRatesData
import kotlin.collections.filter

private const val TAG = "HR_SMOKE"

class RunHeartRateSmokeTests {
    suspend fun runHeartRateSmokeTests(
        healthDataStore: HealthDataStore,
        reader: HeartRateReader
    ) {
        val reader = HeartRateReader(healthDataStore)

        // 1) 당일 데이터
        val today = reader.readToday()
        Log.i(TAG, "[TODAY] count=${today.size}")
        today.take(5).forEachIndexed { i, hr ->
            Log.i(TAG, "[TODAY][$i] uid=${hr.uid} start=${hr.startTime} end=${hr.endTime} bpm=${hr.heartRate} min=${hr.minHeartRate} max=${hr.maxHeartRate}")
        }

        // 품질체크: 시간/범위/값
        validateHr("TODAY", today)

        // 2) 최초 전체 수집 (큰 창) — 진행률 로깅
        val all = reader.readAll(
            yearsPerChunk = 1L,
            onProgress = { done, total -> Log.i(TAG, "[ALL] window $done/$total done") }
        )
        Log.i(TAG, "[ALL] totalCount=${all.size}")
        all.take(5).forEachIndexed { i, hr ->
            Log.i(TAG, "[ALL][$i] uid=${hr.uid} start=${hr.startTime} end=${hr.endTime} bpm=${hr.heartRate}")
        }

        validateHr("ALL", all)

        // 3) 간단 통계
        val avg = all.mapNotNull { it.heartRate as? Number }.map { it.toDouble() }.average()
        val min = all.mapNotNull { it.minHeartRate }.minOrNull()
        val max = all.mapNotNull { it.maxHeartRate }.maxOrNull()
        Log.i(TAG, "[ALL][STATS] avg=${"%.1f".format(avg)} min=$min max=$max")
    }

    private fun validateHr(tag: String, items: List<HeartRatesData>) {
        if (items.isEmpty()) {
            Log.w("HR_SMOKE", "[$tag] empty. 권한/동기화/기간(필터) 확인 필요")
            return
        }
        // 시간 역전/미종료 데이터 검사
        val badTime = items.filter { it.endTime != null && it.startTime?.isAfter(it.endTime) == true }
        if (badTime.isNotEmpty()) Log.w("HR_SMOKE", "[$tag] time order invalid count=${badTime.size}")

        // BPM 범위(대략적): 20~240 밖이면 경고
        val outOfRange = items.filter {
            val bpm = (it.heartRate as? Number)?.toDouble() ?: return@filter false
            bpm < 20.0 || bpm > 240.0
        }
        if (outOfRange.isNotEmpty()) Log.w("HR_SMOKE", "[$tag] out-of-range bpm count=${outOfRange.size}")

        // min/max 논리 체크
        val badMinMax = items.filter {
            val min = (it.minHeartRate as? Number)?.toDouble()
            val max = (it.maxHeartRate as? Number)?.toDouble()
            min != null && max != null && min > max
        }
        if (badMinMax.isNotEmpty()) Log.w("HR_SMOKE", "[$tag] min>max count=${badMinMax.size}")
    }

}

