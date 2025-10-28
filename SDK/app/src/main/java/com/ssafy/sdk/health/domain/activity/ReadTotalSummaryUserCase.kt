package com.ssafy.sdk.health.domain.activity

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.ActivityData
import java.time.LocalDate
import java.time.LocalDateTime
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
        val startDate = LocalDate.now().atStartOfDay()
        val now = LocalDateTime.now()
        val totalActiveTime = readTotalActiveTimeUserCase()
        val totalActiveCaloriesBurned = readTotalActiveCaloriesBurnedUserCase()
        val totalCaloriesBurned = readTotalCaloriesBurnedUserCase()
        val totalDistance = readTotalDistanceUserCase()

        Log.i("Total Active Time: ", "$totalActiveTime")
        return ActivityData(totalActiveCaloriesBurned, totalActiveTime, totalCaloriesBurned, totalDistance)
    }
}