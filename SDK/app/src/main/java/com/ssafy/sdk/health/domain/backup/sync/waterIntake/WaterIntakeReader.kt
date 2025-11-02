package com.ssafy.sdk.health.domain.backup.sync.waterIntake

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.WaterIntakeData
import com.ssafy.sdk.health.domain.waterIntake.ReadGroupedWaterIntakeUseCase
import com.ssafy.sdk.health.domain.waterIntake.ReadTotalWaterIntakeUseCase
import com.ssafy.sdk.health.domain.waterIntake.ReadWaterIntakeGoalUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "WaterIntakeReader"

private val KST: ZoneId = ZoneId.of("Asia/Seoul")


class WaterIntakeReader(
    private val readTotalWaterIntakeUseCase: ReadTotalWaterIntakeUseCase,
    private val readGroupedWaterIntakeUseCase: ReadGroupedWaterIntakeUseCase,
    private val readWaterIntakeGoalUseCase: ReadWaterIntakeGoalUseCase
) {


    // 당일 데이터 수집
    @Throws(HealthDataException::class)
    suspend fun readToday(): WaterIntakeData = withContext(Dispatchers.IO) {
        val start = LocalDate.now(KST).atStartOfDay()
        val end = start.plusDays(1)

        Log.i(TAG, "readToday() start=$start end=$end")

        WaterIntakeData(
            readGroupedWaterIntakeUseCase(),
            readWaterIntakeGoalUseCase(),
            readTotalWaterIntakeUseCase()
        )
    }


}