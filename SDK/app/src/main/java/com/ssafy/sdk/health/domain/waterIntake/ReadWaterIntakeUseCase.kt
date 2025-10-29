package com.ssafy.sdk.health.domain.waterIntake

import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.WaterIntakeData
import com.ssafy.sdk.health.data.model.WaterIntakeGroupedData
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

private const val TAG = "ReadWaterIntakeUseCase"

class ReadWaterIntakeUseCase @Inject constructor(
    private val readTotalWaterIntakeUseCase: ReadTotalWaterIntakeUseCase,
    private val readGroupedWaterIntakeUseCase: ReadGroupedWaterIntakeUseCase,
    private val readWaterIntakeGoalUseCase: ReadWaterIntakeGoalUseCase,
) {

    @Throws(HealthDataException::class)
    suspend operator fun invoke(): WaterIntakeData {
        val zoneOffset = ZoneOffset.of("+09:00")
        val startTime = LocalDate.now(zoneOffset).atStartOfDay()
        val waterIntakes: List<WaterIntakeGroupedData> = readGroupedWaterIntakeUseCase()
        val goal: Float = readWaterIntakeGoalUseCase()
        val total: Float = readTotalWaterIntakeUseCase()

        return WaterIntakeData(startTime, waterIntakes, goal, total)
    }
}