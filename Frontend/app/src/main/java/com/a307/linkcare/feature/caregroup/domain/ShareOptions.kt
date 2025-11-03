package com.a307.linkcare.feature.caregroup.domain

data class ShareOptions(
    val exercise: Boolean,
    val heartRate: Boolean,
    val steps: Boolean,
    val sleep: Boolean,
    val water: Boolean,
    val bloodPressure: Boolean,
    val bloodSugar: Boolean
)