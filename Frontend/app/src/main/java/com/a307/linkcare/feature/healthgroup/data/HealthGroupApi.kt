package com.a307.linkcare.feature.healthgroup.data

import com.a307.linkcare.feature.commongroup.domain.Member
import java.time.LocalDate

object HealthGroupApi {
    suspend fun getWeekData(
        year: Int,
        month: Int,
        weekOfMonth: Int,
        start: LocalDate,
        end: LocalDate
    ): List<Member> {
        // 서버에선 start~end(일~토) 범위를 쓰는 게 안전
        // TODO: 실제 통신
        return emptyList()
    }

    suspend fun getDayData(date: LocalDate): List<Member> {
        // TODO: 실제 통신
        return emptyList()
    }
}