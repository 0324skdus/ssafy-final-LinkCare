package com.a307.linkcare.feature.caregroup.data.model.request

data class CareWeekSummary(
    val dateLabel: String,              // "2025년 10월 25일" 등
    val steps: Int? = null,
    val calories: Int? = null,          // kCal
    val activeMinutes: Int? = null,     // 분

    val systolicAvg: Int? = null,       // 수축기 평균
    val diastolicAvg: Int? = null,      // 이완기 평균
    val morningBp: Pair<Int,Int>? = null,   // (수축, 이완)
    val eveningBp: Pair<Int,Int>? = null,

    val sleepMinutes: Int? = null,      // 총 수면(분)
    val deepMinutes: Int? = null,       // 깊은 수면(분)
    val sleepScore: Int? = null         // 0~100
)