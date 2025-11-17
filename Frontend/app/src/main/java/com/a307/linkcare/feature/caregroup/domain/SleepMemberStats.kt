package com.a307.linkcare.feature.caregroup.domain

data class SleepMemberStats(
    val userSeq: Long,
    val dailySleepMinutes: List<Int>,
    val averageSleepMinutes: Int,
    val totalSleepMinutes: Int
)