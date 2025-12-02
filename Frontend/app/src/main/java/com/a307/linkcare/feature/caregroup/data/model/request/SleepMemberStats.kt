package com.a307.linkcare.feature.caregroup.data.model.request

data class SleepMemberStats(
    val userSeq: Long,
    val dailySleepMinutes: List<Int>,
    val averageSleepMinutes: Int,
    val totalSleepMinutes: Int
)