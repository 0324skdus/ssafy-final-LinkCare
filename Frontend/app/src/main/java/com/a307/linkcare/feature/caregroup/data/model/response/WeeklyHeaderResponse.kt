package com.a307.linkcare.feature.caregroup.data.model.response

import java.time.LocalDateTime

data class WeeklyHeaderResponse(
    val headerMessage: String,
    val generatedAt: LocalDateTime?
)
