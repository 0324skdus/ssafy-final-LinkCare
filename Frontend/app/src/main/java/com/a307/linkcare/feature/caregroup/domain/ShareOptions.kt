package com.a307.linkcare.feature.caregroup.domain

data class ShareOptions(
    val exercise: Boolean = false,
    val heartRate: Boolean = false,
    val steps: Boolean = false,
    val sleep: Boolean = false,
    val water: Boolean = false,
    val bloodPressure: Boolean = false,
    val bloodSugar: Boolean = false
)