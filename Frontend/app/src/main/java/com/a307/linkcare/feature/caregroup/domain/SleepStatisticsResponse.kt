package com.a307.linkcare.feature.caregroup.domain

data class SleepStatisticsResponse(
    val members: List<SleepMemberStats>,
    val avgDuration: Int,          // 전체 평균 수면(분)
    val startDate: String,
    val endDate: String
)