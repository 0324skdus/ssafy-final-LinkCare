package com.ssafy.sdk.health.domain.sleeps

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.test.SleepData
import com.ssafy.sdk.health.data.test.SleepGoalData
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadTotalSleepsUseCase"

class ReadTotalSleepsUseCase @Inject constructor(
    private val readSleepTypeUseCase: ReadSleepTypeUseCase,
    private val readSleepGoalBedTimeUseCase: ReadSleepGoalBedTimeUseCase,
    private val readSleepGoalWakeUpTimeUseCase: ReadSleepGoalWakeUpTimeUseCase,
    private val readTotalSleepDurationUseCase: ReadTotalSleepDurationUseCase,
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): SleepData {
        Log.i(TAG, "ReadTotalSleepsUseCase invoke()")
        val zoneOffset = ZoneOffset.of("+09:00")
        val startTime = LocalDateTime.now(zoneOffset)
        val now = LocalDateTime.now(zoneOffset)
        val sleeps = readSleepTypeUseCase()
        val totalDuration = readTotalSleepDurationUseCase()
        val bedTimeGoal = readSleepGoalBedTimeUseCase()
        val wakeUpGoal = readSleepGoalWakeUpTimeUseCase()

        Log.i("Sleeps: ", "$sleeps")
        Log.i("Total Duration: ", "$totalDuration")
        Log.i("Bed Time Goal: ", "$bedTimeGoal")
        Log.i("Wake up Time Goal: ", "$wakeUpGoal")

        return SleepData(
            startTime,
            sleeps,
            totalDuration,
            SleepGoalData(bedTimeGoal, wakeUpGoal)
        )
    }
}