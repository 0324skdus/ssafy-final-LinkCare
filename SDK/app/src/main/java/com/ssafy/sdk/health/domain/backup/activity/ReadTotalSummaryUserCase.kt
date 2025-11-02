package com.ssafy.sdk.health.domain.activity

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.test.ActivityData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

class ReadTotalSummaryUserCase @Inject constructor(
    private val readTotalActiveTimeUserCase: ReadTotalActiveTimeUserCase,
    private val readTotalActiveCaloriesBurnedUserCase: ReadTotalActiveCaloriesBurnedUserCase,
    private val readTotalCaloriesBurnedUserCase: ReadTotalCaloriesBurnedUserCase,
    private val readTotalDistanceUserCase: ReadTotalDistanceUserCase
)  {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): ActivityData {
        // 한국 시간 기준
        val zoneOffset = ZoneOffset.of("+09:00")

        val startDate = LocalDate.now(zoneOffset).atStartOfDay()
        val now = LocalDateTime.now(zoneOffset)

        val totalActiveTime = readTotalActiveTimeUserCase()
        val totalActiveCaloriesBurned = readTotalActiveCaloriesBurnedUserCase()
        val totalCaloriesBurned = readTotalCaloriesBurnedUserCase()
        val totalDistance = readTotalDistanceUserCase()

        Log.i("Total Active Time: ", "$totalActiveTime")
        Log.i("Total calories burned: ", "$totalCaloriesBurned")
        Log.i("Total distance: ", "$totalDistance")
        return ActivityData(startDate, totalActiveCaloriesBurned, totalActiveTime, totalCaloriesBurned, totalDistance)
    }
}