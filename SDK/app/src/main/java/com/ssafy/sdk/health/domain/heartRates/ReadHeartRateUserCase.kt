package com.ssafy.sdk.health.domain.heartRates

import com.samsung.android.sdk.health.data.error.HealthDataException
import com.ssafy.sdk.health.data.model.HeartRatesData
import javax.inject.Inject
import kotlin.jvm.Throws

class ReadHeartRateUserCase @Inject constructor(

) {
  @Throws(HealthDataException::class)
  suspend operator fun invoke(): HeartRatesData {

  }
}