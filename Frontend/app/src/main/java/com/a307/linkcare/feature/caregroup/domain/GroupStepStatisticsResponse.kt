package com.a307.linkcare.feature.caregroup.domain

data class GroupStepStatisticsResponse(
    val members: List<MemberStep>,
    val totalSteps: Int
)