package com.a307.linkcare.feature.healthgroup.data.model.dto

import com.a307.linkcare.feature.commongroup.domain.model.Member
import com.a307.linkcare.feature.healthgroup.data.model.request.ActualActivity

data class MemberWithActivity(
    val memberInfo: Member,
    val activity: ActualActivity
)