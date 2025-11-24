package com.a307.linkcare.feature.caregroup.data.model.response

import com.a307.linkcare.feature.caregroup.data.model.request.SleepMemberStats

data class SleepStatisticsResponse(
    val members: List<SleepMemberStats>,
    val avgDuration: Int,          // 전체 평균 수면(분)
    val startDate: String,
    val endDate: String
)