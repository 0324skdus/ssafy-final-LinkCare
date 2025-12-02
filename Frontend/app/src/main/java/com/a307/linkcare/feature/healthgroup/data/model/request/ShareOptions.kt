package com.a307.linkcare.feature.healthgroup.data.model.request

data class ShareOptions(
    val exercise: Boolean = false,
    val heartRate: Boolean = false,
    val steps: Boolean = false,
)