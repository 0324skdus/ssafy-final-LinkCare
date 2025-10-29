package com.ssafy.sdk.health.domain.heartRates

import android.util.Log
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.HeartRatesData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.jvm.Throws

class ReadHeartRateUserCase @Inject constructor(
    private val readHeartRateDataUseCase: ReadHeartRateDataUseCase

) {
  @Throws(HealthDataException::class)
  suspend operator fun invoke(): List<HeartRatesData> {
      val zoneOffset = ZoneOffset.of("+09:00")
      val startDate = LocalDate.now(zoneOffset).atStartOfDay()
      val now = LocalDateTime.now(zoneOffset)
      val heartRates: List<HeartRatesData> = readHeartRateDataUseCase()

      Log.i("Heart Rates = ", "$heartRates")
      return heartRates
  }
}