package com.a307.linkcare.feature.caregroup.data.model.response

import com.a307.linkcare.feature.caregroup.data.model.request.MemberStep

data class GroupStepStatisticsResponse(
    val members: List<MemberStep>,
    val totalSteps: Int
)