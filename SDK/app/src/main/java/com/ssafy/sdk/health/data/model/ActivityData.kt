package com.ssafy.sdk.health.data.model

import java.time.LocalDateTime

data class ActivityMinuteBucket(
    val startTime: LocalDateTime,     // 버킷 시작 시각
    val activeCalories: Double?,      // 분(or 시) 단위 활동 칼로리
    val totalCalories: Double?,       // 분(or 시) 단위 전체 칼로리
    val activeTimeMillis: Long?,      // 분(or 시) 단위 활동 시간(밀리초)
    val distanceMeters: Double?
)

data class ActivityData(
    val startTime: LocalDateTime?,
    val totalActiveCaloriesBurned: Double,
    val totalActiveTimeMillis: Long,
    val totalCaloriesBurned: Double,
    val totalDistanceMeters: Double,
//    val minutely: List<ActivityMinuteBucket> = emptyList()
)