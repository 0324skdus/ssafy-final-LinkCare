package com.a307.linkcare.feature.healthgroup.domain

data class ShareOptions(
    val exercise: Boolean = false,
    val heartRate: Boolean = false,
    val steps: Boolean = false,
)